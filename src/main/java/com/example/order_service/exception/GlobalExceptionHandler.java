package com.example.order_service.exception;

import com.example.order_service.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException exception){
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for(FieldError fieldError : exception.getBindingResult().getFieldErrors()){
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "Request validation failed",
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "ORDER_NOT_FOUND",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrder(InvalidOrderException ex) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_ORDER",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_PARAMETER",
                message
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
