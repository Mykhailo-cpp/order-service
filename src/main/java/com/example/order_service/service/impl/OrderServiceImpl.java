package com.example.order_service.service.impl;


import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.kafka.OrderProducer;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderStatus;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;

    @Override
    public OrderCreatedEvent create(OrderRequest orderRequest) {
        Order order = Order.builder()
                .customerId(orderRequest.customerId())
                .product(orderRequest.product())
                .quantity(orderRequest.quantity())
                .status(OrderStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        log.info("Created order with ID: {}", savedOrder.getId());

        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getProduct(),
                savedOrder.getQuantity(),
                savedOrder.getCreatedAt()
        );

        orderProducer.publish(event);

        return event;
    }

    @Override
    @Transactional
    public void updateStatus(UUID orderId, OrderStatus status) {
        Optional<Order> orderOptional = orderRepository.findById(orderId);

        if(orderOptional.isPresent()){
            Order order = orderOptional.get();
            order.setStatus(status);

            log.info("Order {} status updated to {}", orderId, status);
        }
        else{
            log.warn("Order {} not found, ignoring inventory result", orderId);
        }
    }
}
