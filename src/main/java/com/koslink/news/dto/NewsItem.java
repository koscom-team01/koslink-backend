package com.koslink.news.dto;

public record NewsItem(
        String title,
        String originallink,
        String link,
        String description,
        String pubDate
) {
    public NewsItem withCleanHtmlTags() {
        return new NewsItem(
                cleanHtmlTags(title),
                originallink,
                link,
                cleanHtmlTags(description),
                pubDate
        );
    }

    private String cleanHtmlTags(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("</?b>", "");
    }
}
