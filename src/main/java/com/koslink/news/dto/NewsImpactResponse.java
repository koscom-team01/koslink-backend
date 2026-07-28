package com.koslink.news.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 뉴스 영향 분석 응답 DTO
 */
public record NewsImpactResponse(
        @JsonProperty("news_summary")
        List<String> newsSummary,

        NewsSourceDto source,

        @JsonProperty("origin_stocks")
        List<OriginStockDto> originStocks,

        @JsonProperty("related_stocks")
        List<RelatedStockDto> relatedStocks,

        @JsonProperty("final_summary")
        String finalSummary,

        NewsGraphDto graph
) {
}
