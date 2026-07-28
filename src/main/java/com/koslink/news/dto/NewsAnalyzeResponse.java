package com.koslink.news.dto;

/**
 * 뉴스 분석 응답
 *
 * @param newsId 뉴스 ID
 * @param status 상태 ("accepted")
 */
public record NewsAnalyzeResponse(
        Long newsId,
        String status
) {
}
