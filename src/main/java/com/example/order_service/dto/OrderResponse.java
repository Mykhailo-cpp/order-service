package com.example.order_service.dto;

import com.example.order_service.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "An order as returned by the API")
public record OrderResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(example = "42")
        Long customerId,

        @Schema(example = "laptop")
        String product,

        @Schema(example = "3")
        Integer quantity,

        @Schema(description = "NEW until the inventory service answers", example = "NEW")
        OrderStatus status,

        @Schema(example = "2026-11-17T14:00:00")
        LocalDateTime createdAt
) {
}