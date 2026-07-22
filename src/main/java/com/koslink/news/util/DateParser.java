package com.koslink.news.util;

import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 네이버 뉴스 API pubDate 파싱 유틸리티
 * 형식: "Mon, 21 Jan 2024 10:00:00 +0900"
 */
@Slf4j
public class DateParser {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    public static OffsetDateTime parseNaverPubDate(String pubDate) {
        try {
            return OffsetDateTime.parse(pubDate, FORMATTER);
        } catch (Exception e) {
            log.error("Failed to parse pubDate: {}", pubDate, e);
            // 파싱 실패 시 현재 시간 반환
            return OffsetDateTime.now();
        }
    }
}
