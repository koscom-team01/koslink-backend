package com.koslink.news.dto;

import java.util.List;

/**
 * 뉴스 그래프 DTO
 */
public record NewsGraphDto(
        String newsId,
        String originId,
        List<GraphNodeDto> nodes,
        List<GraphEdgeDto> edges
) {
}
