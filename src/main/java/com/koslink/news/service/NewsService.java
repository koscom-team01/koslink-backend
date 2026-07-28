package com.koslink.news.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.koslink.news.cache.ArticleFingerprint;
import com.koslink.news.client.NaverNewsClient;
import com.koslink.news.crawler.NaverNewsCrawler;
import com.koslink.news.dto.CrawledArticle;
import com.koslink.news.dto.CrawledNews;
import com.koslink.news.dto.NewsItem;
import com.koslink.news.dto.NewsSearchRequest;
import com.koslink.news.dto.NewsSearchResponse;
import com.koslink.news.util.NaverNewsUrlFilter;
import com.koslink.news.util.TitleSimilarity;
import com.koslink.support.naver.NaverApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class NewsService {

    private final NaverNewsClient naverNewsClient;
    private final NaverApiProperties naverApiProperties;
    private final NaverNewsCrawler naverNewsCrawler;

    public NewsSearchResponse searchNews(NewsSearchRequest request) {
        log.info("Searching news with query: {}", request.query());

        NewsSearchResponse response = naverNewsClient.searchNews(
                naverApiProperties.clientId(),
                naverApiProperties.clientSecret(),
                request.query(),
                request.display(),
                request.start(),
                request.sort()
        );

        log.info("Found {} news articles", response.total());

        // HTML 태그 제거한 응답 반환
        return response.withCleanHtmlTags();
    }

    /**
     * 네이버 뉴스 URL만 필터링
     *
     * @param items 뉴스 아이템 목록
     * @return 네이버 뉴스 URL만 포함된 목록
     */
    public List<NewsItem> filterNaverNewsUrls(List<NewsItem> items) {
        return items.stream()
                .filter(item -> NaverNewsUrlFilter.isNaverNewsUrl(item.link()))
                .toList();
    }

    /**
     * Jaccard 유사도로 중복 제거
     *
     * @param items 검사할 뉴스 아이템 목록
     * @param cache 중복 체크용 캐시
     * @param threshold 유사도 임계값 (0.0 ~ 1.0)
     * @return 중복이 아닌 기사 목록
     */
    public List<NewsItem> filterDuplicates(
            List<NewsItem> items,
            Cache<String, ArticleFingerprint> cache,
            double threshold
    ) {
        List<NewsItem> uniqueItems = new ArrayList<>();
        int duplicateCount = 0;

        for (NewsItem item : items) {
            Set<String> newTokens = TitleSimilarity.tokenize(item.title());

            if (isDuplicate(newTokens, item.title(), cache, threshold)) {
                duplicateCount++;
                continue;
            }

            // 중복이 아니면 캐시 등록 및 리스트 추가
            registerToCache(item, newTokens, cache);
            uniqueItems.add(item);
        }

        log.info("Duplicate filtering completed - unique: {}, duplicate: {}, cache size: {}",
                uniqueItems.size(), duplicateCount, cache.estimatedSize());

        return uniqueItems;
    }

    /**
     * 기사 크롤링
     *
     * @param items 크롤링할 뉴스 아이템 목록
     * @return 크롤링 성공한 기사 목록
     */
    public List<CrawledArticle> crawlArticles(List<NewsItem> items) {
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
     * @param cache 중복 체크용 캐시
     * @param threshold 유사도 임계값
     * @return 중복이면 true, 아니면 false
     */
    public boolean isDuplicate(
            Set<String> newTokens,
            String title,
            Cache<String, ArticleFingerprint> cache,
            double threshold
    ) {
        for (ArticleFingerprint cached : cache.asMap().values()) {
            double similarity = TitleSimilarity.jaccard(newTokens, cached.getTitleTokens());

            if (similarity >= threshold) {
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
     * @param cache 등록할 캐시
     */
    public void registerToCache(
            NewsItem item,
            Set<String> tokens,
            Cache<String, ArticleFingerprint> cache
    ) {
        ArticleFingerprint fingerprint = new ArticleFingerprint(
                item.link(),
                item.title(),
                tokens,
                LocalDateTime.now()
        );
        cache.put(item.link(), fingerprint);
    }
}
