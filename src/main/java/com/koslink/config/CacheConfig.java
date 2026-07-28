package com.koslink.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.koslink.news.cache.ArticleFingerprint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 캐시 설정
 * 기사 중복 판별을 위한 인메모리 캐시
 */
@Configuration
public class CacheConfig {

    /**
     * 최근 기사 캐시 (실시간 폴링용)
     * - TTL: 24시간 (expireAfterWrite, 롤링 방식)
     * - 최대 크기: 5000개
     */
    @Bean
    public Cache<String, ArticleFingerprint> recentArticleCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .maximumSize(5000)
                .build();
    }

    /**
     * 뉴스 코퍼스 백필용 캐시
     * - TTL: 2시간 (백필 작업 완료까지 유지)
     * - 최대 크기: 무제한 (메모리 상한 없음, 백필 중 약 5000건 예상)
     */
    @Bean
    public Cache<String, ArticleFingerprint> newsCorpusArticleCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.HOURS)
                .build();
    }
}
