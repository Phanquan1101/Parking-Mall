package com.parkflow.mall.parking.exception;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExitPassExceptionHandler {
    @ExceptionHandler(ExitPassException.class)
    ResponseEntity<Map<String, String>> handle(ExitPassException exception) {
        return ResponseEntity.status(exception.status())
                .body(Map.of("errorCode", exception.errorCode(), "message", exception.getMessage()));
    }
}
