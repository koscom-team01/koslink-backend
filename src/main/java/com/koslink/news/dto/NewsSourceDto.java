package com.koslink.news.dto;

/**
 * 뉴스 출처 정보 DTO
 */
public record NewsSourceDto(
        String press,
        String publishedAt,
        String url
) {
}
