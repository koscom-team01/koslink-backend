package com.koslink.news.scheduler;

import com.github.benmanes.caffeine.cache.Cache;
import com.koslink.news.cache.ArticleFingerprint;
import com.koslink.news.crawler.NaverNewsCrawler;
import com.koslink.news.dto.CrawledArticle;
import com.koslink.news.dto.CrawledNews;
import com.koslink.news.dto.NewsItem;
import com.koslink.news.dto.NewsSearchRequest;
import com.koslink.news.dto.NewsSearchResponse;
import com.koslink.news.entity.News;
import com.koslink.news.repository.NewsRepository;
import com.koslink.news.service.NewsService;
import com.koslink.news.util.DateParser;
import com.koslink.news.util.NaverNewsUrlFilter;
import com.koslink.news.util.TitleSimilarity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class NewsScheduler {

    private static final String KEYWORD = "반도체";
    private static final int DISPLAY_SIZE = 100;
    private static final double SIMILARITY_THRESHOLD = 0.4;

    private final NewsService newsService;
    private final Cache<String, ArticleFingerprint> recentArticleCache;
    private final NaverNewsCrawler naverNewsCrawler;
    private final NewsRepository newsRepository;

    /**
     * 마지막으로 처리한 기사의 link (커서)
     * 단일 스레드 스케줄러이므로 volatile로 충분
     */
    private volatile String lastSeenLink = null;

    /**
     * 신규 기사만 추출 (커서 방식)
     * lastSeenLink가 나오기 전까지의 기사들만 반환
     *
     * @param items API 응답 (최신순)
     * @param lastSeenLink 이전 폴링의 마지막 link
     * @return 신규 기사 목록
     */
    private List<NewsItem> extractNewItems(List<NewsItem> items, String lastSeenLink) {
        if (lastSeenLink == null) {
            // 최초 실행: 전체 반환
            log.info("First polling - returning all {} items", items.size());
            return new ArrayList<>(items);
        }

        List<NewsItem> newItems = new ArrayList<>();
        for (NewsItem item : items) {
            if (item.link().equals(lastSeenLink)) {
                // 이전 폴링에서 처리한 기사 발견 → 중단
                break;
            }
            newItems.add(item);
        }

        log.info("Extracted {} new items (lastSeenLink: {})", newItems.size(), lastSeenLink);
        return newItems;
    }

    /**
     * 캐시 워밍업 (최초 실행 시)
     * 최초 100개 안에도 중복이 있을 수 있으므로 중복 판별 수행
     * 크롤링 및 DB 저장도 함께 수행
     */
    private void warmupCache(List<NewsItem> items) {
        log.info("Cache warmup - processing {} items with duplicate check", items.size());

        // 최초 실행이지만 일반 실행과 동일하게 처리
        processDuplicateCheck(items);

        log.info("Cache warmup completed - cache size: {}", recentArticleCache.estimatedSize());
    }

    /**
     * 중복 판별 및 처리
     * Jaccard 유사도로 같은 사건인지 판단 후 크롤링 및 DB 저장
     */
    private void processDuplicateCheck(List<NewsItem> items) {
        // 1단계: 중복 제거
        List<NewsItem> uniqueItems = filterDuplicates(items);

        // 2단계: 크롤링
        List<CrawledArticle> crawledArticles = crawlArticles(uniqueItems);

        // 3단계: DB 저장
        if (!crawledArticles.isEmpty()) {
            saveCrawledArticlesToDb(crawledArticles);
        }
    }

    /**
     * Jaccard 유사도로 중복 제거
     *
     * @param items 검사할 뉴스 아이템 목록
     * @return 중복이 아닌 기사 목록
     */
    private List<NewsItem> filterDuplicates(List<NewsItem> items) {
        List<NewsItem> uniqueItems = new ArrayList<>();
        int duplicateCount = 0;

        for (NewsItem item : items) {
            Set<String> newTokens = TitleSimilarity.tokenize(item.title());

            if (isDuplicateArticle(newTokens, item.title())) {
                duplicateCount++;
                continue;
            }

            // 중복이 아니면 캐시 등록 및 리스트 추가
            registerToCache(item, newTokens);
            uniqueItems.add(item);
        }

        log.info("Duplicate filtering completed - unique: {}, duplicate: {}, cache size: {}",
                uniqueItems.size(), duplicateCount, recentArticleCache.estimatedSize());

        return uniqueItems;
    }

    /**
     * 기사 크롤링
     *
     * @param items 크롤링할 뉴스 아이템 목록
     * @return 크롤링 성공한 기사 목록
     */
    private List<CrawledArticle> crawlArticles(List<NewsItem> items) {
        List<CrawledArticle> crawledArticles = new ArrayList<>();

        for (NewsItem item : items) {
            log.info("Crawling article: [{}]", item.title());

            Optional<CrawledNews> crawledOpt = naverNewsCrawler.crawl(item.link());

            if (crawledOpt.isPresent()) {
                CrawledNews crawled = crawledOpt.get();
                log.info("Crawling success: [{}] (body: {} chars, press: {})",
                        item.title(), crawled.body().length(), crawled.press());

                crawledArticles.add(new CrawledArticle(item, crawled));
            } else {
                log.warn("Crawling failed, skip article: [{}] {}", item.title(), item.link());
            }
        }

        log.info("Crawling completed - success: {}/{}", crawledArticles.size(), items.size());

        return crawledArticles;
    }

    /**
     * 캐시의 기사들과 Jaccard 유사도 비교하여 중복 여부 판단
     *
     * @param newTokens 새 기사의 토큰 집합
     * @param title 새 기사 제목
     * @return 중복이면 true, 아니면 false
     */
    private boolean isDuplicateArticle(Set<String> newTokens, String title) {
        for (ArticleFingerprint cached : recentArticleCache.asMap().values()) {
            double similarity = TitleSimilarity.jaccard(newTokens, cached.getTitleTokens());

            if (similarity >= SIMILARITY_THRESHOLD) {
                log.info("Duplicate detected (similarity: {:.2f}): [{}] vs [{}]",
                        similarity, title, cached.getNormalizedTitle());
                return true;
            }
        }
        return false;
    }

    /**
     * 캐시에 기사 등록
     *
     * @param item 뉴스 아이템
     * @param tokens 토큰 집합
     */
    private void registerToCache(NewsItem item, Set<String> tokens) {
        ArticleFingerprint fingerprint = new ArticleFingerprint(
                item.link(),
                item.title(),
                tokens,
                LocalDateTime.now()
        );
        recentArticleCache.put(item.link(), fingerprint);
    }

    /**
     * 크롤링된 기사들을 DB에 배치 저장
     * DB에 이미 존재하는 URL은 제외
     *
     * @param crawledArticles 크롤링된 기사 목록
     */
    @Transactional
    private void saveCrawledArticlesToDb(List<CrawledArticle> crawledArticles) {
        // URL 중복 체크 (배치 조회)
        List<String> urls = crawledArticles.stream()
                .map(ca -> ca.item().link())
                .toList();

        Set<String> existingUrls = newsRepository.findExistingUrls(urls);
        log.info("DB duplicate check: {} crawled, {} already exist", urls.size(), existingUrls.size());

        // DB에 없는 기사만 배치 저장
        List<News> newsToSave = crawledArticles.stream()
                .filter(ca -> !existingUrls.contains(ca.item().link()))
                .map(this::convertToNewsEntity)
                .toList();

        if (!newsToSave.isEmpty()) {
            try {
                newsRepository.saveAll(newsToSave);
                log.info("DB batch saved: {} articles", newsToSave.size());
            } catch (Exception e) {
                log.error("Failed to batch save to DB", e);
            }
        }
    }

    /**
     * CrawledArticle을 News 엔티티로 변환
     *
     * @param crawledArticle 크롤링된 기사
     * @return News 엔티티
     */
    private News convertToNewsEntity(CrawledArticle crawledArticle) {
        OffsetDateTime publishedAt = DateParser.parseNaverPubDate(crawledArticle.item().pubDate());
        return News.of(
                crawledArticle.item().title(),
                crawledArticle.crawledNews().body(),
                crawledArticle.item().link(),
                crawledArticle.crawledNews().press(),
                publishedAt
        );
    }

    /**
     * 1분마다 반도체 뉴스 100개 폴링
     * cron: 초 분 시 일 월 요일
     * "0 * * * * *" = 매분 0초에 실행
     */
    @Scheduled(cron = "0 * * * * *")
    public void fetchNews() {
        log.info("=== News polling started: keyword={}, display={} ===", KEYWORD, DISPLAY_SIZE);

        try {
            // 1. API 호출
            NewsSearchResponse response = fetchNewsFromApi();

            if (response.items().isEmpty()) {
                log.warn("No items in API response");
                return;
            }

            // 2. 신규 기사 추출 및 필터링
            List<NewsItem> naverNewsItems = extractAndFilterNewItems(response.items());

            // 3. 중복 판별 및 처리
            processNewItems(naverNewsItems);

            // 4. 커서 갱신
            updateLastSeenLink(response.items().get(0).link());

        } catch (Exception e) {
            log.error("Failed to fetch news: {}", e.getMessage(), e);
        }

        log.info("=== News polling completed ===");
    }

    /**
     * 네이버 뉴스 API 호출
     *
     * @return API 응답
     */
    private NewsSearchResponse fetchNewsFromApi() {
        NewsSearchRequest request = NewsSearchRequest.of(KEYWORD, DISPLAY_SIZE, 1, "date");
        NewsSearchResponse response = newsService.searchNews(request);

        log.info("API response: {} items (total: {})", response.items().size(), response.total());
        return response;
    }

    /**
     * 신규 기사 추출 및 네이버 뉴스 URL 필터링
     *
     * @param items API 응답 아이템 목록
     * @return 필터링된 네이버 뉴스 아이템 목록
     */
    private List<NewsItem> extractAndFilterNewItems(List<NewsItem> items) {
        // 신규 기사 추출
        List<NewsItem> newItems = extractNewItems(items, lastSeenLink);

        // 네이버 뉴스 URL 필터링
        List<NewsItem> naverNewsItems = newItems.stream()
                .filter(item -> NaverNewsUrlFilter.isNaverNewsUrl(item.link()))
                .toList();

        int filteredCount = newItems.size() - naverNewsItems.size();
        if (filteredCount > 0) {
            log.info("Filtered out {} non-Naver news URLs", filteredCount);
        }

        return naverNewsItems;
    }

    /**
     * 신규 기사 처리 (최초 실행 vs 일반 실행 분기)
     *
     * @param naverNewsItems 필터링된 네이버 뉴스 아이템 목록
     */
    private void processNewItems(List<NewsItem> naverNewsItems) {
        if (lastSeenLink == null) {
            // 최초 실행: 캐시 워밍업만 수행
            warmupCache(naverNewsItems);
        } else if (naverNewsItems.isEmpty()) {
            log.info("No new Naver news items to process");
        } else {
            processDuplicateCheck(naverNewsItems);
        }
    }

    /**
     * 커서 갱신
     *
     * @param latestLink 최신 기사의 link
     */
    private void updateLastSeenLink(String latestLink) {
        lastSeenLink = latestLink;
        log.info("Updated lastSeenLink: {}", lastSeenLink);
    }
}
