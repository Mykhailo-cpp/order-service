package com.example.order_service.service;

import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.dto.PageResponse;
import com.example.order_service.kafka.OrderProducer;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderStatus;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderProducer orderProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void create_savesOrderAsNewAndPublishesEvent() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(id);
            o.setCreatedAt(createdAt);
            return o;
        });

        OrderCreatedEvent result = orderService.create(new OrderRequest(7L, "keyboard", 2));

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(saved.getValue().getCustomerId()).isEqualTo(7L);
        assertThat(saved.getValue().getProduct()).isEqualTo("keyboard");
        assertThat(saved.getValue().getQuantity()).isEqualTo(2);
        assertThat(saved.getValue().getCreatedAt()).isNotNull();

        OrderCreatedEvent expected = new OrderCreatedEvent(id, 7L, "keyboard", 2, createdAt);
        assertThat(result).isEqualTo(expected);
        verify(orderProducer).publishCreated(expected);
    }

    @Test
    void create_whenSaveFails_doesNotPublish() {
        when(orderRepository.save(any(Order.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> orderService.create(new OrderRequest(1L, "mouse", 1)))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(orderProducer);
    }

    @Test
    void updateStatus_existingOrder_changesStatus() {
        UUID id = UUID.randomUUID();
        Order order = Order.builder().id(id).status(OrderStatus.NEW).build();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        orderService.updateStatus(id, OrderStatus.AVAILABLE);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.AVAILABLE);
    }

    @Test
    void updateStatus_unknownOrder_isIgnored() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        orderService.updateStatus(id, OrderStatus.OUT_OF_STOCK);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void findAll_withoutStatus_usesFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Order order = Order.builder().id(UUID.randomUUID()).customerId(1L).product("p").quantity(1)
                .status(OrderStatus.NEW).createdAt(LocalDateTime.now()).build();
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order), pageable, 41));

        PageResponse<OrderResponse> result = orderService.findAll(null, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(41);
        assertThat(result.totalPages()).isEqualTo(3);
        verify(orderRepository, never()).findByStatus(any(), any());
    }

    @Test
    void findAll_withStatus_usesFindByStatus() {
        Pageable pageable = PageRequest.of(1, 10);
        when(orderRepository.findByStatus(OrderStatus.AVAILABLE, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<OrderResponse> result = orderService.findAll(OrderStatus.AVAILABLE, pageable);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(1);
        verify(orderRepository, never()).findAll(any(Pageable.class));
    }
}
