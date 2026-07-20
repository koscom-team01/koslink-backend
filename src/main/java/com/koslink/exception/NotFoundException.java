package com.koslink.exception;

public class NotFoundException extends KoslinkException {
    public NotFoundException(String message, String displayMessage) {
        super(message, displayMessage);
    }

    public NotFoundException(String message) {
        super(message);
    }
}
