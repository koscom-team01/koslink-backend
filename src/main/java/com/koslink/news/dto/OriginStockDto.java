package com.koslink.news.dto;

/**
 * 원천 종목 DTO
 */
public record OriginStockDto(
        String ticker,
        String name,
        String status,
        String relationLabel,
        String relationPath,
        String propagation
) {
}
