package com.koslink.exception;

/**
 * JSON 파싱 실패 예외
 */
public class JsonParsingException extends KoslinkException {
    public JsonParsingException() {
        super("Failed to parse JSON data", "데이터 파싱 중 오류가 발생했습니다.");
    }
}
