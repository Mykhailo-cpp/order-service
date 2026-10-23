package com.example.order_service.service;

import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.model.OrderStatus;

import java.util.UUID;

public interface OrderService {
    OrderCreatedEvent create(OrderRequest orderRequest);

    void updateStatus(UUID orderId, OrderStatus status);
}
