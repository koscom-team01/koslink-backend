package com.koslink.exception;

/**
 * 뉴스를 찾을 수 없는 경우 예외
 */
public class NewsNotFoundException extends NotFoundException {
    public NewsNotFoundException() {
        super("News not found", "뉴스를 찾을 수 없습니다.");
    }
}
