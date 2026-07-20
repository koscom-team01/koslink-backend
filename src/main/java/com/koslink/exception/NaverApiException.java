package com.koslink.exception;

public class NaverApiException extends KoslinkException {
    public NaverApiException(String message, String displayMessage) {
        super(message, displayMessage);
    }

    public NaverApiException(String message) {
        super(message, "네이버 API 호출 중 오류가 발생했습니다.");
    }
}
