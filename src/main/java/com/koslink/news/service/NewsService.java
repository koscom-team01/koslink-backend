package com.koslink.news.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.koslink.common.cache.ArticleFingerprint;
import com.koslink.corpus.repository.NewsCorpusRepository;
import com.koslink.exception.AiResponseNotFoundException;
import com.koslink.exception.JsonParsingException;
import com.koslink.exception.NewsNotAnalyzedException;
import com.koslink.exception.NewsNotFoundException;
import com.koslink.news.client.NaverNewsClient;
import com.koslink.news.crawler.NaverNewsCrawler;
import com.koslink.news.dto.*;
import com.koslink.news.entity.AiResponse;
import com.koslink.news.entity.News;
import com.koslink.news.entity.NewsStatus;
import com.koslink.news.repository.AiResponseRepository;
import com.koslink.news.repository.NewsRepository;
import com.koslink.news.util.NaverNewsUrlFilter;
import com.koslink.news.util.TitleSimilarity;
import com.koslink.support.naver.NaverApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final NewsCorpusRepository newsCorpusRepository;
    private final NewsRepository newsRepository;
    private final AiResponseRepository aiResponseRepository;
    private final ObjectMapper objectMapper;

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

    /**
     * 최신 뉴스 리스트 조회 (커서 기반 페이지네이션)
     * status = DONE인 뉴스만 반환
     *
     * @param cursorId 커서 ID (null이면 첫 페이지)
     * @param size     페이지당 개수
     * @return 뉴스 리스트 응답
     */
    @Transactional(readOnly = true)
    public com.koslink.news.dto.NewsListResponse getNewsList(Long cursorId, int size) {
        List<News> newsList = findNewsList(cursorId, size);
        boolean hasNext = checkHasNext(newsList, size);
        List<News> slicedList = sliceBySize(newsList, size, hasNext);

        List<NewsItemDto> newsItems = slicedList.stream()
                .map(NewsItemDto::from)
                .toList();

        return NewsListResponse.of(newsItems, hasNext, getLastCursorId(slicedList));
    }

    private List<News> findNewsList(Long cursorId, int size) {
        Pageable pageable = PageRequest.of(0, size + 1);

        if (cursorId == null) {
            return newsRepository.findByStatusOrderByPublishedAtDesc(NewsStatus.DONE, pageable);
        }
        return newsRepository.findByStatusAndNewsIdLessThanOrderByPublishedAtDesc(
                NewsStatus.DONE,
                cursorId,
                pageable
        );
    }

    private boolean checkHasNext(List<News> newsList, int size) {
        return newsList.size() > size;
    }

    private List<News> sliceBySize(
            List<News> newsList,
            int size,
            boolean hasNext
    ) {
        if (!hasNext) {
            return newsList;
        }
        return newsList.subList(0, size);
    }

    private Long getLastCursorId(List<News> slicedList) {
        if (slicedList.isEmpty()) {
            return null;
        }
        return slicedList.get(slicedList.size() - 1).getNewsId();
    }

    /**
     * 뉴스 영향 분석 조회
     *
     * @param newsId 뉴스 ID
     * @return 뉴스 영향 분석 결과
     */
    @Transactional(readOnly = true)
    public NewsImpactResponse getNewsImpact(Long newsId) {
        // 1. news 테이블에서 조회
        News news = newsRepository.findById(newsId)
                .orElseThrow(NewsNotFoundException::new);

        // 2. status = DONE 확인
        if (news.getStatus() != NewsStatus.DONE) {
            throw new NewsNotAnalyzedException();
        }

        // 3. ai_responses 테이블에서 조회
        AiResponse aiResponse = aiResponseRepository.findByNewsId(newsId)
                .orElseThrow(AiResponseNotFoundException::new);

        // 4. 데이터 파싱 및 DTO 변환
        return buildNewsImpactResponse(news, aiResponse);
    }

    private NewsImpactResponse buildNewsImpactResponse(News news, AiResponse aiResponse) {
        try {
            // newsSummary 파싱 (jsonb → List<String>)
            List<String> newsSummary = objectMapper.readValue(
                    aiResponse.getNewsSummary(),
                    new TypeReference<>() {}
            );

            // source 파싱 (jsonb → NewsSourceDto)
            NewsSourceDto source = objectMapper.readValue(
                    aiResponse.getSource(),
                    NewsSourceDto.class
            );

            // originStocks 파싱 (jsonb → List<OriginStockDto>)
            List<OriginStockDto> originStocks = objectMapper.readValue(
                    aiResponse.getOriginStocks(),
                    new TypeReference<>() {}
            );

            // relatedStocks 파싱 (jsonb → List<RelatedStockDto>)
            // null이거나 빈 배열이면 빈 리스트 반환
            List<RelatedStockDto> relatedStocks = parseRelatedStocks(aiResponse.getRelatedStocks());

            // graph 파싱 (jsonb → NewsGraphDto)
            NewsGraphDto graph = objectMapper.readValue(
                    aiResponse.getGraph(),
                    NewsGraphDto.class
            );

            return new NewsImpactResponse(
                    newsSummary,
                    source,
                    originStocks,
                    relatedStocks,
                    aiResponse.getFinalSummary(),
                    graph
            );
        } catch (JsonProcessingException e) {
            throw new JsonParsingException();
        }
    }

    private List<RelatedStockDto> parseRelatedStocks(String relatedStocksJson) {
        if (relatedStocksJson == null || relatedStocksJson.isBlank()) {
            return List.of();
        }

        try {
            List<RelatedStockDto> relatedStocks = objectMapper.readValue(
                    relatedStocksJson,
                    new TypeReference<>() {}
            );
            return relatedStocks != null ? relatedStocks : List.of();
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse related_stocks, returning empty list");
            return List.of();
        }
    }
}
