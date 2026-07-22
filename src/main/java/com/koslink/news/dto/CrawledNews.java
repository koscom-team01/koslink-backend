package com.koslink.news.dto;

/**
 * 크롤링된 뉴스 데이터 (본문 + 언론사명)
 */
public record CrawledNews(String body, String press) {
}
