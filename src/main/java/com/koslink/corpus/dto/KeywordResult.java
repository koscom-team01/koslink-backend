package com.koslink.corpus.dto;

/**
 * 키워드 처리 결과
 */
public record KeywordResult(
        int collected,
        int skippedByUrl,
        int skippedBySimilarity,
        int crawlFailed
) {
}
