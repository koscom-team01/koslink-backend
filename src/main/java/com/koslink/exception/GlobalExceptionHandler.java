package com.koslink.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기
 *
 * 애플리케이션 전체에서 발생하는 예외들을 통일된 형태로 처리하여
 * 클라이언트에게 일관된 오류 응답을 제공합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 404 Not Found 예외 처리
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(NotFoundException e) {
        return new ErrorResponse("NOT_FOUND", e.getDisplayMessage());
    }

    /**
     * 400 Bad Request 예외 처리
     */
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequestException(BadRequestException e) {
        return new ErrorResponse("BAD_REQUEST", e.getDisplayMessage());
    }

    /**
     * 500 Internal Server Error 예외 처리
     */
    @ExceptionHandler(KoslinkException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleKoslinkException(KoslinkException e) {
        String message = e.getDisplayMessage() != null ? e.getDisplayMessage() : "알 수 없는 에러가 발생했습니다.";
        return new ErrorResponse("INTERNAL_SERVER_ERROR", message);
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException e) {
        return new ErrorResponse("BAD_REQUEST", e.getLocalizedMessage());
    }

    /**
     * 예상치 못한 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception e) {
        log.error("Unexpected exception occurred", e);
        return new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");
    }
}
