package com.koslink.news.dto;

/**
 * 그래프 노드 DTO
 */
public record GraphNodeDto(
        String id,
        String name,
        String kind,
        String ticker,
        String sector,
        Long marketCap,
        String direction
) {
}
