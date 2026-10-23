package com.example.order_service.controller;

import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.exception.GlobalExceptionHandler;
import com.example.order_service.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createOrder_validRequest_returns202WithEvent() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.create(any(OrderRequest.class))).thenReturn(
                new OrderCreatedEvent(orderId, 42L, "laptop", 3, LocalDateTime.now()));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 42, "product": "laptop", "quantity": 3}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value(42))
                .andExpect(jsonPath("$.product").value("laptop"))
                .andExpect(jsonPath("$.quantity").value(3));

        verify(orderService).create(new OrderRequest(42L, "laptop", 3));
    }

    @Test
    void createOrder_invalidFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": null, "product": "  ", "quantity": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.customerId").exists())
                .andExpect(jsonPath("$.product").exists())
                .andExpect(jsonPath("$.quantity").exists());

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_negativeQuantity_returns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "product": "phone", "quantity": -5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.quantity").exists());

        verify(orderService, never()).create(any());
    }

    @Test
    void createOrder_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }
}