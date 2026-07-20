package com.koslink.exception;

public class InvalidStartRangeException extends BadRequestException {
    public InvalidStartRangeException() {
        super("start must be between 1 and 1000", "start는 1에서 1000 사이의 값이어야 합니다.");
    }
}
