package com.koslink.news.controller;

import com.koslink.news.dto.NewsSearchRequest;
import com.koslink.news.dto.NewsSearchResponse;
import com.koslink.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsService newsService;

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
