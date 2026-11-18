package com.example.order_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Kafka event ORDER_UPDATED, published to topic order-events (key: customerId)")
public record OrderUpdatedEvent(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID orderId,

        @Schema(example = "laptop")
        String product,

        @Schema(example = "5")
        Integer newQuantity,

        @Schema(example = "2026-11-17T14:05:00.000")
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        LocalDateTime createdTime
) {
}