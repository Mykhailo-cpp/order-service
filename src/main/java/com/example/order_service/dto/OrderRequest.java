package com.example.order_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRequest(
        @NotNull
        Long customerId,

        @NotBlank
        String product,

        @NotNull
        @Positive
        Integer quantity
) {
}
