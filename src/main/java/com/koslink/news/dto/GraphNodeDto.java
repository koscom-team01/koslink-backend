package com.koslink.news.dto;

/**
 * 그래프 노드 DTO
 */
public record GraphNodeDto(
        String id,
        String name,
        String ticker,
        String capSize,
        String marketType
) {
}
