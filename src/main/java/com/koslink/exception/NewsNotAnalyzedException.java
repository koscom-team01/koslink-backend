package com.koslink.exception;

/**
 * 뉴스 분석이 완료되지 않은 경우 예외
 */
public class NewsNotAnalyzedException extends BadRequestException {
    public NewsNotAnalyzedException() {
        super("News analysis not completed yet", "뉴스 분석이 아직 완료되지 않았습니다.");
    }
}
