package com.koslink.news.client;

import com.koslink.news.dto.NewsSearchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "naver-news-client",
        url = "${naver.api.base-url}"
)
public interface NaverNewsClient {

    @GetMapping("${naver.api.news-search-path}")
    NewsSearchResponse searchNews(
            @RequestHeader("X-Naver-Client-Id") String clientId,
            @RequestHeader("X-Naver-Client-Secret") String clientSecret,
            @RequestParam("query") String query,
            @RequestParam(value = "display", required = false) Integer display,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "sort", required = false) String sort
    );
}
