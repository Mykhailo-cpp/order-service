package com.example.order_service.dto;

import com.example.order_service.model.OrderStatus;

import java.util.UUID;

public record InventoryResultEvent(UUID orderId, OrderStatus status) {
}
