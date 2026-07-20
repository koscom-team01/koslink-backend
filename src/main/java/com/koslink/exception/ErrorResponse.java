package com.koslink.exception;

public record ErrorResponse(
        String errorCode,
        String message
) {
}
