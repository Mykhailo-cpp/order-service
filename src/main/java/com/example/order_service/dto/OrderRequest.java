package com.example.order_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Payload for creating an order")
public record OrderRequest(
        @Schema(description = "Id of the customer placing the order", example = "123")
        @NotNull(message = "Customer ID is required")
        Long customerId,

        @Schema(description = "Product name", example = "laptop")
        @NotBlank(message = "Product name is required")
        String product,

        @Schema(description = "Quantity of the product", example = "1")
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        Integer quantity
) {
}
