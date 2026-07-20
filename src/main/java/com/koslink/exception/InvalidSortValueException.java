package com.koslink.exception;

public class InvalidSortValueException extends BadRequestException {
    public InvalidSortValueException() {
        super("sort must be 'sim' or 'date'", "sort는 'sim' 또는 'date'만 가능합니다.");
    }
}
