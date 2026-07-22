package com.koslink.news.dto;

/**
 * 크롤링 완료된 기사 저장용 DTO
 */
public record CrawledArticle(NewsItem item, CrawledNews crawledNews) {
}
