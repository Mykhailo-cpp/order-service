package com.example.order_service.dto;

public record UpdateOrderRequest(
        String product,
        Integer newQuantity
) {
}
