package com.koslink.exception;

/**
 * 뉴스 필터링 실패 시 예외
 */
public class NewsFilterException extends KoslinkException {
    public NewsFilterException() {
        super("Failed to filter news", "뉴스 필터링 중 오류가 발생했습니다.");
    }
}
