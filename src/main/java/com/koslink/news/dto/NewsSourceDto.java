package com.koslink.news.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 뉴스 출처 정보 DTO
 */
public record NewsSourceDto(
        String press,
        @JsonProperty("published_at")
        String publishedAt,
        String url
) {
}
