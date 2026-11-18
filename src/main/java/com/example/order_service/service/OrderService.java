package com.example.order_service.service;

import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.dto.PageResponse;
import com.example.order_service.model.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    OrderCreatedEvent create(OrderRequest orderRequest);

    void updateStatus(UUID orderId, OrderStatus status);

    PageResponse<OrderResponse> findAll(OrderStatus status, Pageable pageable);
}
