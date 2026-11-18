package com.example.order_service.mapper;

import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

    @Test
    void toEntity_copiesRequestFieldsAndLeavesServerManagedFieldsEmpty() {
        Order order = mapper.toEntity(new OrderRequest(5L, "monitor", 2));

        assertThat(order.getCustomerId()).isEqualTo(5L);
        assertThat(order.getProduct()).isEqualTo("monitor");
        assertThat(order.getQuantity()).isEqualTo(2);
        assertThat(order.getId()).isNull();
        assertThat(order.getStatus()).isNull();
        assertThat(order.getCreatedAt()).isNull();
    }

    @Test
    void toResponse_mapsEveryField() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder().id(id).customerId(5L).product("monitor").quantity(2)
                .status(OrderStatus.OUT_OF_STOCK).createdAt(now).build();

        assertThat(mapper.toResponse(order))
                .isEqualTo(new OrderResponse(id, 5L, "monitor", 2, OrderStatus.OUT_OF_STOCK, now));
    }

    @Test
    void toCreatedEvent_renamesIdAndCreatedAt() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder().id(id).customerId(5L).product("monitor").quantity(2)
                .status(OrderStatus.NEW).createdAt(now).build();

        assertThat(mapper.toCreatedEvent(order))
                .isEqualTo(new OrderCreatedEvent(id, 5L, "monitor", 2, now));
    }

    @Test
    void nullInput_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }
}