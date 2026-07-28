package com.koslink.exception;

public class BadRequestException extends KoslinkException {
    public BadRequestException(String message, String displayMessage) {
        super(message, displayMessage);
    }
}
