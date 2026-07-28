package com.koslink.corpus.dto;

import java.util.Map;

/**
 * 백필 결과
 */
public record BackfillResult(
        Map<String, Integer> collectedByKeyword,
        int totalCollected,
        int totalSkippedByUrl,
        int totalSkippedBySimilarity,
        int totalCrawlFailed,
        long durationMs
) {
}
