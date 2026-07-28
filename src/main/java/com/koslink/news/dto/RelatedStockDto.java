package com.koslink.news.dto;

/**
 * 관련 종목 DTO (온톨로지 파생)
 */
public record RelatedStockDto(
        String ticker,
        String name,
        String status,
        String relationLabel,
        String relationPath,
        String propagation
) {
}
