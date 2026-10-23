package com.example.order_service.kafka;

import com.example.order_service.dto.InventoryResultEvent;
import com.example.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryResultListener {

    private final OrderService orderService;

    @KafkaListener(topics = "inventory-result", groupId = "order-service")
    public void handleInventoryResult(InventoryResultEvent event){
        log.info("Received inventory result {} for order {}", event.status(), event.orderId());

        orderService.updateStatus(event.orderId(), event.status());
    }
}
