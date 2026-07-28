package com.koslink.news.dto;

import java.util.List;

/**
 * 뉴스 리스트 응답 (커서 기반 페이지네이션)
 */
public record NewsListResponse(
        List<NewsItemDto> news,
        boolean hasNext,
        Long lastCursorId
) {
    public static NewsListResponse of(List<NewsItemDto> news, boolean hasNext, Long lastCursorId) {
        return new NewsListResponse(news, hasNext, lastCursorId);
    }
}
