package com.example.order_service.kafka;

import com.example.order_service.dto.InventoryResultEvent;
import com.example.order_service.model.OrderStatus;
import com.example.order_service.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryResultListenerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private InventoryResultListener listener;

    @Test
    void handleInventoryResult_delegatesToOrderService() {
        UUID id = UUID.randomUUID();

        listener.handleInventoryResult(new InventoryResultEvent(id, OrderStatus.AVAILABLE));

        verify(orderService).updateStatus(id, OrderStatus.AVAILABLE);
    }
}