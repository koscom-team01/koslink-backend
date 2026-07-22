package com.koslink.news.util;

import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

/**
 * 네이버 뉴스 API pubDate 파싱 유틸리티
 * 형식: "Mon, 21 Jan 2024 10:00:00 +0900"
 */
@Slf4j
public class DateParser {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    /**
     * 네이버 뉴스 API의 pubDate 문자열을 OffsetDateTime으로 파싱
     *
     * @param pubDate RFC 1123 형식의 날짜 문자열
     * @return 파싱된 OffsetDateTime (파싱 실패 시 Optional.empty())
     */
    public static Optional<OffsetDateTime> parseNaverPubDate(String pubDate) {
        try {
            return Optional.of(OffsetDateTime.parse(pubDate, FORMATTER));
        } catch (DateTimeParseException e) {
            log.error("Failed to parse pubDate: '{}' - {}", pubDate, e.getMessage());
            return Optional.empty();
        }
    }
}
