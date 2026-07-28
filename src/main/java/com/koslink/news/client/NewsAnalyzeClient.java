package com.koslink.news.client;

import com.koslink.news.dto.NewsAnalyzeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 뉴스 분석 API 클라이언트
 */
@FeignClient(
        name = "newsAnalyzeClient",
        url = "${news.analyze.base-url}"
)
public interface NewsAnalyzeClient {

    /**
     * PENDING 상태 뉴스 분석 요청
     *
     * @param limit 분석할 뉴스 개수 (1~100)
     * @return 선점된 뉴스 목록
     */
    @PostMapping("/api/v1/news/analyze-pending")
    List<NewsAnalyzeResponse> analyzePendingNews(@RequestParam(defaultValue = "20") int limit);
}
