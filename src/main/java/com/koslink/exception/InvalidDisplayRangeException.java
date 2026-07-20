package com.koslink.exception;

public class InvalidDisplayRangeException extends BadRequestException {
    public InvalidDisplayRangeException() {
        super("display must be between 1 and 100", "display는 1에서 100 사이의 값이어야 합니다.");
    }
}
