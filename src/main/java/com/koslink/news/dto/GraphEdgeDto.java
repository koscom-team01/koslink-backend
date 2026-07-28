package com.koslink.news.dto;

/**
 * 그래프 엣지 DTO
 */
public record GraphEdgeDto(
        String id,
        String source,
        String target,
        String relation
) {
}
