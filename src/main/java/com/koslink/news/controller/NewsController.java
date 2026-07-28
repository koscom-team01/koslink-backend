package com.koslink.news.controller;

import com.koslink.news.dto.*;
import com.koslink.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsService newsService;

    /**
     * 최신 뉴스 리스트 조회 (커서 기반 페이지네이션)
     *
     * @param cursorId 커서 ID (없으면 첫 페이지)
     * @param size     페이지당 개수 (기본값: 20)
     * @return 뉴스 리스트 응답
     */
    @GetMapping
    public NewsListResponse getNewsList(
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/v1/news - cursorId: {}, size: {}", cursorId, size);
        return newsService.getNewsList(cursorId, size);
    }

    /**
     * 뉴스 영향 분석 조회
     *
     * @param newsId 뉴스 ID
     * @return 뉴스 영향 분석 결과
     */
    @GetMapping("/{newsId}/impact")
    public NewsImpactResponse getNewsImpact(@PathVariable Long newsId) {
        log.info("GET /api/v1/news/{}/impact", newsId);
        return newsService.getNewsImpact(newsId);
    }

    @GetMapping("/search")
    public ResponseEntity<NewsSearchResponse> searchNews(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "10") Integer display,
            @RequestParam(required = false, defaultValue = "1") Integer start,
            @RequestParam(required = false, defaultValue = "sim") String sort
    ) {
        NewsSearchRequest request = NewsSearchRequest.of(query, display, start, sort);
        NewsSearchResponse response = newsService.searchNews(request);
        return ResponseEntity.ok(response);
    }
}
