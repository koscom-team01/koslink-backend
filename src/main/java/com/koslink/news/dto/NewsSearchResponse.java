package com.koslink.news.dto;

import java.util.Collections;
import java.util.List;

public record NewsSearchResponse(
        String lastBuildDate,
        Integer total,
        Integer start,
        Integer display,
        List<NewsItem> items
) {
    public NewsSearchResponse withCleanHtmlTags() {
        if (items == null) {
            return new NewsSearchResponse(lastBuildDate, total, start, display, Collections.emptyList());
        }

        List<NewsItem> cleanedItems = items.stream()
                .map(NewsItem::withCleanHtmlTags)
                .toList();
        return new NewsSearchResponse(lastBuildDate, total, start, display, cleanedItems);
    }
}
