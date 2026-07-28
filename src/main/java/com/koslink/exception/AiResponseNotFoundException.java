package com.koslink.exception;

/**
 * AI 응답이 존재하지 않는 경우 예외
 */
public class AiResponseNotFoundException extends NotFoundException {
    public AiResponseNotFoundException() {
        super("AI response not found", "AI 분석 결과를 찾을 수 없습니다.");
    }
}
