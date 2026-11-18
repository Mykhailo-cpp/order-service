package com.example.order_service.dto;

import com.example.order_service.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Kafka message consumed from topic inventory-result; sets the order status")
public record InventoryResultEvent(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID orderId,

        @Schema(example = "AVAILABLE")
        OrderStatus status
) {
}