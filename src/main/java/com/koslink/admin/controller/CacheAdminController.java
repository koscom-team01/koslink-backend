package com.koslink.admin.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.koslink.news.cache.ArticleFingerprint;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 캐시 관리 Admin API
 * 개발/디버깅 용도로 캐시 내용 확인
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/cache")
public class CacheAdminController {

    private final Cache<String, ArticleFingerprint> recentArticleCache;

    /**
     * 캐시 통계 조회
     * GET /api/v1/admin/cache/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", recentArticleCache.estimatedSize());
        stats.put("totalEntries", recentArticleCache.asMap().size());

        return ResponseEntity.ok(stats);
    }

    /**
     * 캐시 전체 조회
     * GET /api/v1/admin/cache/entries
     */
    @GetMapping("/entries")
    public ResponseEntity<List<Map<String, Object>>> getCacheEntries() {
        List<Map<String, Object>> entries = recentArticleCache.asMap().values().stream()
                .map(fingerprint -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("link", fingerprint.getLink());
                    entry.put("normalizedTitle", fingerprint.getNormalizedTitle());
                    entry.put("tokenCount", fingerprint.getTitleTokens().size());
                    entry.put("pubDate", fingerprint.getPubDate());
                    return entry;
                })
                .toList();

        return ResponseEntity.ok(entries);
    }

    /**
     * 캐시 비우기
     * DELETE /api/v1/admin/cache
     */
    @GetMapping("/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        long beforeSize = recentArticleCache.estimatedSize();
        recentArticleCache.invalidateAll();
        recentArticleCache.cleanUp();

        Map<String, String> result = new HashMap<>();
        result.put("message", "Cache cleared successfully");
        result.put("clearedCount", String.valueOf(beforeSize));

        return ResponseEntity.ok(result);
    }
}
