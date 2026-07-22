package com.koslink.news.util;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * 네이버 뉴스 URL 필터링 유틸리티
 * 네이버 뉴스 도메인인지 확인하여 크롤링 대상 여부 판별
 */
@Slf4j
public class NaverNewsUrlFilter {

    private static final Set<String> NAVER_NEWS_DOMAINS = Set.of(
            "news.naver.com",
            "n.news.naver.com"
    );

    /**
     * 네이버 뉴스 URL인지 확인
     *
     * @param link 기사 URL
     * @return 네이버 뉴스 도메인이면 true, 아니면 false
     */
    public static boolean isNaverNewsUrl(String link) {
        if (link == null || link.isBlank()) {
            log.warn("Empty link provided");
            return false;
        }

        try {
            URI uri = new URI(link);
            String host = uri.getHost();

            if (host == null) {
                log.warn("No host in URL: {}", link);
                return false;
            }

            boolean isNaverNews = NAVER_NEWS_DOMAINS.stream()
                    .anyMatch(host::endsWith);

            log.debug("URL filter check: {} -> {}", link, isNaverNews);
            return isNaverNews;

        } catch (URISyntaxException e) {
            log.warn("Invalid URI syntax: {}", link, e);
            return false;
        }
    }
}
