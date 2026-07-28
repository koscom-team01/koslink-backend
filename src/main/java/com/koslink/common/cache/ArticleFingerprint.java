package com.koslink.common.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 기사 지문 (중복 판별용)
 * Caffeine 캐시에 저장되는 데이터
 */
@Getter
@AllArgsConstructor
public class ArticleFingerprint {
    /**
     * 기사 URL (고유 식별자)
     */
    private final String link;

    /**
     * 정규화된 제목 (표시용)
     */
    private final String normalizedTitle;

    /**
     * 제목 토큰 (Jaccard 유사도 계산용)
     */
    private final Set<String> titleTokens;

    /**
     * 발행 시각
     */
    private final LocalDateTime pubDate;
}
