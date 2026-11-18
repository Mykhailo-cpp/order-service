package com.example.order_service.service.impl;

import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.dto.PageResponse;
import com.example.order_service.kafka.OrderProducer;
import com.example.order_service.mapper.OrderMapper;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderStatus;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final OrderMapper orderMapper;

    @Override
    public OrderResponse create(OrderRequest orderRequest) {
        Order order = orderMapper.toEntity(orderRequest);
        order.setStatus(OrderStatus.NEW);
        order.setCreatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        log.info("Created order with ID: {}", savedOrder.getId());

        orderProducer.publishCreated(orderMapper.toCreatedEvent(savedOrder));

        return orderMapper.toResponse(savedOrder);
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

    @Override
    public PageResponse<OrderResponse> findAll(OrderStatus status, Pageable pageable) {
        Page<Order> orders;
        if (status == null) {
            orders = orderRepository.findAll(pageable);
        } else {
            orders = orderRepository.findByStatus(status, pageable);
        }

        return PageResponse.from(orders.map(orderMapper::toResponse));
    }
}
