package com.koslink.exception;

import lombok.Getter;

@Getter
public class KoslinkException extends RuntimeException {
    private final String displayMessage;

    public KoslinkException(String message, String displayMessage) {
        super(message);
        this.displayMessage = displayMessage;
    }

    public KoslinkException(String message) {
        super(message);
        this.displayMessage = null;
    }
}
