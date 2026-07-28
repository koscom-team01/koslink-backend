package com.koslink.corpus.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.koslink.corpus.dto.BackfillResult;
import com.koslink.corpus.dto.KeywordResult;
import com.koslink.corpus.dto.NewsCorpusCacheDto;
import com.koslink.corpus.entity.NewsCorpus;
import com.koslink.corpus.entity.NewsDuplicates;
import com.koslink.corpus.repository.NewsCorpusRepository;
import com.koslink.corpus.repository.NewsDuplicatesRepository;
import com.koslink.common.cache.ArticleFingerprint;
import com.koslink.news.dto.CrawledArticle;
import com.koslink.news.dto.NewsItem;
import com.koslink.news.dto.NewsSearchRequest;
import com.koslink.news.dto.NewsSearchResponse;
import com.koslink.news.service.NewsService;
import com.koslink.news.util.DateParser;
import com.koslink.news.util.TitleSimilarity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * 뉴스 코퍼스 백필 서비스
 * RAG 학습용 과거 뉴스 데이터 수집
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class NewsCorpusBackfillService {

    private static final List<String> BASE_KEYWORDS = List.of(
            "HBM",
            "파운드리",
            "SK하이닉스",
            "삼성전자",
            "반도체"
    );

    private static final List<String> MONTHS = List.of(
            "1월", "2월", "3월", "4월", "5월", "6월",
            "7월", "8월", "9월", "10월", "11월", "12월"
    );

    /**
     * 키워드 생성 (키워드 × 월)
     * 예: HBM 1월, HBM 2월, ..., 반도체 12월
     */
    private static List<String> generateKeywords() {
        List<String> keywords = new ArrayList<>();
        for (String baseKeyword : BASE_KEYWORDS) {
            for (String month : MONTHS) {
                keywords.add(baseKeyword + " " + month);
            }
        }
        return keywords;
    }

    private static final List<String> KEYWORDS = generateKeywords();

    private static final int DISPLAY_SIZE = 100;
    private static final int MAX_PAGES = 10;
    private static final double SIMILARITY_THRESHOLD = 0.4;
    private static final long API_DELAY_MS = 500;

    private final NewsService newsService;
    private final NewsCorpusRepository newsCorpusRepository;
    private final NewsDuplicatesRepository newsDuplicatesRepository;
    private final Cache<String, ArticleFingerprint> newsCorpusArticleCache;

    /**
     * 전체 백필 실행
     * 60개 키워드를 순서대로 처리 (5개 키워드 × 12개월)
     *
     * @return 백필 결과
     */
    public BackfillResult backfillAll() {
        log.info("=== News Corpus Backfill Started ({} keywords) ===", KEYWORDS.size());
        long startTime = System.currentTimeMillis();

        // 캐시 워밍업
        warmupCache();

        Map<String, Integer> collectedByKeyword = new LinkedHashMap<>();
        int totalSkippedByUrl = 0;
        int totalSkippedBySimilarity = 0;
        int totalCrawlFailed = 0;

        for (String keyword : KEYWORDS) {
            log.info("Processing keyword: {}", keyword);
            KeywordResult result = backfillByKeyword(keyword);

            collectedByKeyword.put(keyword, result.collected());
            totalSkippedByUrl += result.skippedByUrl();
            totalSkippedBySimilarity += result.skippedBySimilarity();
            totalCrawlFailed += result.crawlFailed();

            log.info("Keyword [{}] completed - collected: {}, url skip: {}, similarity skip: {}, crawl failed: {}",
                    keyword, result.collected(), result.skippedByUrl(), result.skippedBySimilarity(), result.crawlFailed());
        }

        long durationMs = System.currentTimeMillis() - startTime;
        int totalCollected = collectedByKeyword.values().stream().mapToInt(Integer::intValue).sum();

        log.info("=== News Corpus Backfill Completed ===");
        log.info("Total collected: {}, Duration: {}ms", totalCollected, durationMs);

        return new BackfillResult(
                collectedByKeyword,
                totalCollected,
                totalSkippedByUrl,
                totalSkippedBySimilarity,
                totalCrawlFailed,
                durationMs
        );
    }

    /**
     * 캐시 워밍업
     * DB에 저장된 모든 뉴스 코퍼스를 캐시에 로드
     */
    private void warmupCache() {
        log.info("Cache warmup started");
        List<NewsCorpusCacheDto> existingCorpus = newsCorpusRepository.findAllForCacheWarmup();

        for (NewsCorpusCacheDto corpus : existingCorpus) {
            Set<String> tokens = TitleSimilarity.tokenize(corpus.title());
            ArticleFingerprint fingerprint = new ArticleFingerprint(
                    corpus.url(),
                    corpus.title(),
                    tokens,
                    LocalDateTime.now()
            );
            newsCorpusArticleCache.put(corpus.url(), fingerprint);
        }

        log.info("Cache warmup completed - loaded {} articles", existingCorpus.size());
    }

    /**
     * 키워드별 백필 실행
     *
     * @param keyword 검색 키워드
     * @return 키워드 처리 결과
     */
    private KeywordResult backfillByKeyword(String keyword) {
        int collected = 0;
        int skippedByUrl = 0;
        int skippedBySimilarity = 0;
        int crawlFailed = 0;

        for (int page = 0; page < MAX_PAGES; page++) {
            int start = page * DISPLAY_SIZE + 1;

            try {
                // API 호출
                NewsSearchRequest request = NewsSearchRequest.of(keyword, DISPLAY_SIZE, start, "date");
                NewsSearchResponse response = newsService.searchNews(request);

                if (response.items().isEmpty()) {
                    log.info("No more items for keyword [{}] at page {}", keyword, page + 1);
                    break;
                }

                // 네이버 뉴스 URL 필터링
                List<NewsItem> naverNewsItems = newsService.filterNaverNewsUrls(response.items());

                log.info("Page {}/{} - fetched: {}, naver news: {}",
                        page + 1, MAX_PAGES, response.items().size(), naverNewsItems.size());

                // 1단계: 유사도 중복 체크 (캐시)
                List<NewsItem> uniqueItems = new ArrayList<>();
                for (NewsItem item : naverNewsItems) {
                    // 유사도 중복 체크
                    Set<String> newTokens = TitleSimilarity.tokenize(item.title());
                    if (newsService.isDuplicate(
                            newTokens, item.title(), newsCorpusArticleCache, SIMILARITY_THRESHOLD)) {
                        skippedBySimilarity++;

                        // 유사도 중복으로 제외된 기사를 news_duplicates에 저장
                        saveToDuplicates(item, keyword);

                        continue;
                    }

                    // 즉시 캐시 등록 (다음 페이지에서 유사도 판별용)
                    newsService.registerToCache(item, newTokens, newsCorpusArticleCache);

                    uniqueItems.add(item);
                }

                // 2단계: 크롤링
                List<CrawledArticle> crawledArticles = newsService.crawlArticles(uniqueItems);
                crawlFailed += (uniqueItems.size() - crawledArticles.size());

                // 3단계: DB 저장
                for (CrawledArticle crawledArticle : crawledArticles) {
                    // pubDate 파싱
                    Optional<OffsetDateTime> publishedAtOpt = DateParser.parseNaverPubDate(
                            crawledArticle.item().pubDate());
                    if (publishedAtOpt.isEmpty()) {
                        log.warn("Date parse failed, skipping: {}", crawledArticle.item().link());
                        continue;
                    }

                    // 저장
                    Set<String> tokens = TitleSimilarity.tokenize(crawledArticle.item().title());
                    NewsCorpus corpus = NewsCorpus.of(
                            crawledArticle.item().title(),
                            crawledArticle.crawledNews().body(),
                            crawledArticle.item().link(),
                            crawledArticle.crawledNews().press(),
                            publishedAtOpt.get()
                    );

                    newsCorpusRepository.save(corpus);
                    collected++;
                    log.info("Saved: [{}]", corpus.getTitle());
                }

                // API Rate Limiting
                if (page < MAX_PAGES - 1) {
                    Thread.sleep(API_DELAY_MS);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Backfill interrupted for keyword [{}]", keyword);
                break;
            } catch (Exception e) {
                log.error("Error processing keyword [{}] page {}: {}", keyword, page + 1, e.getMessage(), e);
            }
        }

        return new KeywordResult(collected, skippedByUrl, skippedBySimilarity, crawlFailed);
    }

    /**
     * 유사도 중복으로 제외된 기사를 news_duplicates에 저장
     *
     * @param item    뉴스 아이템
     * @param keyword 검색 키워드
     */
    private void saveToDuplicates(NewsItem item, String keyword) {
        try {
            // pubDate 파싱
            Optional<OffsetDateTime> pubDateOpt = DateParser.parseNaverPubDate(item.pubDate());
            if (pubDateOpt.isEmpty()) {
                log.debug("Skip saving to news_duplicates - pubDate parse failed: {}", item.link());
                return;
            }

            NewsDuplicates newsDuplicates = NewsDuplicates.of(
                    item.link(),
                    item.title(),
                    pubDateOpt.get(),
                    keyword
            );

            newsDuplicatesRepository.save(newsDuplicates);
            log.debug("Saved to news_duplicates (similarity skip): [{}]", item.title());
        } catch (Exception e) {
            // 저장 실패 시 무시
            log.trace("Failed to save to news_duplicates: {}", e.getMessage());
        }
    }
}
