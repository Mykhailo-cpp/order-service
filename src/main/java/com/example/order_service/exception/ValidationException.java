package com.example.order_service.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class ValidationException extends RuntimeException {
    private final Map<String, String> errors;

    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public ValidationException(String field, String message) {
        this(message, Map.of(field, message));
    }
}
