package com.koslink.news.scheduler;

import com.github.benmanes.caffeine.cache.Cache;
import com.koslink.news.cache.ArticleFingerprint;
import com.koslink.news.dto.NewsItem;
import com.koslink.news.dto.NewsSearchRequest;
import com.koslink.news.dto.NewsSearchResponse;
import com.koslink.news.service.NewsService;
import com.koslink.news.util.NaverNewsUrlFilter;
import com.koslink.news.util.TitleSimilarity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
     * AI 분석/크롤링은 스킵
     */
    private void warmupCache(List<NewsItem> items) {
        log.info("Cache warmup - processing {} items with duplicate check", items.size());

        // 최초 실행이지만 중복 판별은 수행 (내부적으로 캐시에 등록됨)
        processDuplicateCheck(items);

        log.info("Cache warmup completed - cache size: {}", recentArticleCache.estimatedSize());
    }

    /**
     * 중복 판별 및 처리
     * Jaccard 유사도로 같은 사건인지 판단
     */
    private void processDuplicateCheck(List<NewsItem> items) {
        int newArticleCount = 0;
        int duplicateCount = 0;

        for (NewsItem item : items) {
            Set<String> newTokens = TitleSimilarity.tokenize(item.title());

            // 캐시의 모든 기사와 유사도 비교
            boolean isDuplicate = false;
            for (ArticleFingerprint cached : recentArticleCache.asMap().values()) {
                double similarity = TitleSimilarity.jaccard(newTokens, cached.getTitleTokens());

                if (similarity >= SIMILARITY_THRESHOLD) {
                    // 같은 사건으로 판단 → skip
                    log.info("Duplicate detected (similarity: {:.2f}): [{}] vs [{}]",
                            similarity, item.title(), cached.getNormalizedTitle());
                    isDuplicate = true;
                    duplicateCount++;
                    break;
                }
            }

            if (!isDuplicate) {
                // 새로운 사건 → 캐시 등록 + 크롤링 대상
                ArticleFingerprint fingerprint = new ArticleFingerprint(
                        item.link(),
                        item.title(),  // 원본 제목 저장
                        newTokens,
                        LocalDateTime.now()
                );

                recentArticleCache.put(item.link(), fingerprint);
                newArticleCount++;

                log.info("New article detected: [{}]", item.title());
                // TODO: Phase 5 - 크롤링 및 DB 저장 로직 추가 예정
            }
        }

        log.info("Duplicate check completed - new: {}, duplicate: {}, cache size: {}",
                newArticleCount, duplicateCount, recentArticleCache.estimatedSize());
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
            NewsSearchRequest request = NewsSearchRequest.of(KEYWORD, DISPLAY_SIZE, 1, "date");
            NewsSearchResponse response = newsService.searchNews(request);

            log.info("API response: {} items (total: {})", response.items().size(), response.total());

            if (response.items().isEmpty()) {
                log.warn("No items in API response");
                return;
            }

            // 2. 신규 기사 추출
            List<NewsItem> newItems = extractNewItems(response.items(), lastSeenLink);

            // 3. 네이버 뉴스 URL 필터링
            List<NewsItem> naverNewsItems = newItems.stream()
                    .filter(item -> NaverNewsUrlFilter.isNaverNewsUrl(item.link()))
                    .toList();

            int filteredCount = newItems.size() - naverNewsItems.size();
            if (filteredCount > 0) {
                log.info("Filtered out {} non-Naver news URLs", filteredCount);
            }

            // 4. 중복 판별 및 처리
            if (lastSeenLink == null) {
                // 최초 실행: 캐시 워밍업만 수행
                warmupCache(naverNewsItems);
            } else if (naverNewsItems.isEmpty()) {
                log.info("No new Naver news items to process");
            } else {
                processDuplicateCheck(naverNewsItems);
            }

            // 3. 커서 갱신 (항상 최신 기사의 link로 갱신)
            lastSeenLink = response.items().get(0).link();
            log.info("Updated lastSeenLink: {}", lastSeenLink);

        } catch (Exception e) {
            log.error("Failed to fetch news: {}", e.getMessage(), e);
        }

        log.info("=== News polling completed ===");
    }
}
