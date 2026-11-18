package com.example.order_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API error response")
public record ErrorResponse(
        @Schema(example = "2026-11-17T14:00:00Z")
        Instant timestamp,

        @Schema(example = "404")
        int status,

        @Schema(example = "ORDER_NOT_FOUND")
        String error,

        @Schema(example = "Order with id 3fa85f64-5717-4562-b3fc-2c963f66afa6 was not found")
        String message,

        @Schema(description = "Field-level validation errors, if applicable")
        Map<String, String> validationErrors
) {

    public ErrorResponse(Instant timestamp, int status, String error, String message) {
        this(timestamp, status, error, message, null);
    }
}