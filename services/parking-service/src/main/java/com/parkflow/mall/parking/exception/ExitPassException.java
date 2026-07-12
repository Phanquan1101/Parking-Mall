package com.parkflow.mall.parking.exception;

import org.springframework.http.HttpStatus;

public class ExitPassException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public ExitPassException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus status() {
        return status;
    }

    public String errorCode() {
        return errorCode;
    }
}
