package com.example.order_service.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class OrderNotFoundException extends RuntimeException{

    private final UUID orderId;

    public OrderNotFoundException(UUID orderId) {
        super("Order not found with id: " + orderId);
        this.orderId = orderId;
    }
}
