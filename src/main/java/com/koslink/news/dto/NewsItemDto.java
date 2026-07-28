package com.koslink.news.dto;

import com.koslink.corpus.entity.NewsCorpus;
import com.koslink.news.entity.News;

/**
 * 뉴스 아이템 DTO
 */
public record NewsItemDto(
        Long newsId,
        String title,
        String press,
        String publishedAt,
        String url
) {
    public static NewsItemDto from(NewsCorpus newsCorpus) {
        return new NewsItemDto(
                newsCorpus.getNewsCorpusId(),
                newsCorpus.getTitle(),
                newsCorpus.getPress(),
                newsCorpus.getPublishedAt().toString(),
                newsCorpus.getUrl()
        );
    }

    public static NewsItemDto from(News news) {
        return new NewsItemDto(
                news.getNewsId(),
                news.getTitle(),
                news.getPress(),
                news.getPublishedAt().toString(),
                news.getUrl()
        );
    }
}
