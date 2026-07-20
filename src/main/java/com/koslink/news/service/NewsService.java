package com.koslink.news.service;

import com.koslink.news.client.NaverNewsClient;
import com.koslink.news.dto.NewsSearchRequest;
import com.koslink.news.dto.NewsSearchResponse;
import com.koslink.support.naver.NaverApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class NewsService {

    private final NaverNewsClient naverNewsClient;
    private final NaverApiProperties naverApiProperties;

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
}
