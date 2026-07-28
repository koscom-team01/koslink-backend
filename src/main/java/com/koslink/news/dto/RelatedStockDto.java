package com.koslink.news.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 관련 종목 DTO (온톨로지 파생)
 */
public record RelatedStockDto(
        String ticker,
        String name,
        String status,
        @JsonProperty("relation_label")
        String relationLabel,
        @JsonProperty("relation_path")
        String relationPath,
        String propagation
) {
}
