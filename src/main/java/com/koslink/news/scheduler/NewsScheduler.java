package com.koslink.news.scheduler;

import com.github.benmanes.caffeine.cache.Cache;
import com.koslink.common.cache.ArticleFingerprint;
import com.koslink.news.dto.CrawledArticle;
import com.koslink.news.dto.NewsItem;
import com.koslink.news.dto.NewsSearchRequest;
import com.koslink.news.dto.NewsSearchResponse;
import com.koslink.news.entity.News;
import com.koslink.news.repository.NewsRepository;
import com.koslink.news.client.NewsAnalyzeClient;
import com.koslink.news.dto.NewsAnalyzeResponse;
import com.koslink.news.service.NewsFilterService;
import com.koslink.news.service.NewsService;
import com.koslink.news.util.DateParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final NewsFilterService newsFilterService;
    private final NewsAnalyzeClient newsAnalyzeClient;
    private final Cache<String, ArticleFingerprint> recentArticleCache;
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
     * 중복 판별 및 처리
     * Jaccard 유사도로 같은 사건인지 판단 후 LLM 필터링, 크롤링 및 DB 저장
     */
    private void processDuplicateCheck(List<NewsItem> items) {
        if (items.isEmpty()) {
            log.info("No items to process");
            return;
        }
        // 1단계: 중복 제거
        List<NewsItem> uniqueItems = newsService.filterDuplicates(
                items, recentArticleCache, SIMILARITY_THRESHOLD);

        // 2단계: LLM 필터링
        List<NewsItem> relevantItems = filterByLlm(uniqueItems);

        // 3단계: 크롤링
        List<CrawledArticle> crawledArticles = newsService.crawlArticles(relevantItems);

        // 4단계: DB 저장
        if (!crawledArticles.isEmpty()) {
            saveCrawledArticlesToDb(crawledArticles);
        }
    }

    /**
     * LLM을 사용하여 반도체 산업 관련 뉴스만 필터링 (배치 처리)
     * 필터링 실패 시 보수적으로 전체 포함
     *
     * @param items 중복 제거된 뉴스 아이템 목록
     * @return 반도체 산업 관련 뉴스만 포함된 목록
     */
    private List<NewsItem> filterByLlm(List<NewsItem> items) {
        if (items.isEmpty()) {
            return items;
        }

        try {
            // 제목 목록 추출
            List<String> titles = items.stream()
                    .map(NewsItem::title)
                    .toList();

            // LLM으로 배치 필터링
            List<String> relevantTitles = newsFilterService.filterRelevantTitles(titles);

            // 관련 제목에 해당하는 NewsItem만 반환
            List<NewsItem> relevantItems = items.stream()
                    .filter(item -> relevantTitles.contains(item.title()))
                    .toList();

            int filteredCount = items.size() - relevantItems.size();
            log.info("LLM batch filtering completed - relevant: {}, filtered: {}",
                    relevantItems.size(), filteredCount);

            return relevantItems;
        } catch (Exception e) {
            // LLM 필터링 실패 시 보수적으로 전체 포함
            log.warn("LLM batch filtering failed, including all items by default: {}", e.getMessage());
            return items;
        }
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

        // DB에 없는 기사만 변환 (pubDate 파싱 실패 시 제외)
        List<News> newsToSave = crawledArticles.stream()
                .filter(ca -> !existingUrls.contains(ca.item().link()))
                .map(this::convertToNewsEntity)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (!newsToSave.isEmpty()) {
            try {
                newsRepository.saveAll(newsToSave);
                log.info("DB batch saved: {} articles", newsToSave.size());

                // 분석 API 호출
                requestNewsAnalysis(newsToSave.size());
            } catch (Exception e) {
                log.error("Failed to batch save to DB", e);
            }
        }
    }

    /**
     * CrawledArticle을 News 엔티티로 변환
     *
     * @param crawledArticle 크롤링된 기사
     * @return News 엔티티 (pubDate 파싱 실패 시 Optional.empty())
     */
    private Optional<News> convertToNewsEntity(CrawledArticle crawledArticle) {
        Optional<OffsetDateTime> publishedAtOpt = DateParser.parseNaverPubDate(crawledArticle.item().pubDate());

        if (publishedAtOpt.isEmpty()) {
            log.warn("Skipping article due to pubDate parse failure: [{}] {}",
                    crawledArticle.item().title(), crawledArticle.item().pubDate());
            return Optional.empty();
        }

        News news = News.of(
                crawledArticle.item().title(),
                crawledArticle.crawledNews().body(),
                crawledArticle.item().link(),
                crawledArticle.crawledNews().press(),
                publishedAtOpt.get()
        );

        return Optional.of(news);
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
        List<NewsItem> naverNewsItems = newsService.filterNaverNewsUrls(newItems);

        int filteredCount = newItems.size() - naverNewsItems.size();
        if (filteredCount > 0) {
            log.info("Filtered out {} non-Naver news URLs", filteredCount);
        }

        return naverNewsItems;
    }

    /**
     * 신규 기사 처리
     *
     * @param naverNewsItems 필터링된 네이버 뉴스 아이템 목록
     */
    private void processNewItems(List<NewsItem> naverNewsItems) {
        processDuplicateCheck(naverNewsItems);
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

    /**
     * 뉴스 분석 API 호출
     * PENDING 상태 뉴스를 분석 요청
     *
     * @param savedCount 저장된 뉴스 개수
     */
    private void requestNewsAnalysis(int savedCount) {
        List<NewsAnalyzeResponse> responses = newsAnalyzeClient.analyzePendingNews(savedCount);
    }
}
