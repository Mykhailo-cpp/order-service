package com.example.order_service.controller;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.dto.PageResponse;
import com.example.order_service.exception.GlobalExceptionHandler;
import com.example.order_service.model.OrderStatus;
import com.example.order_service.service.OrderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private OrderResponse sampleOrder(UUID id) {
        return new OrderResponse(id, 42L, "laptop", 3, OrderStatus.NEW, LocalDateTime.of(2026, 11, 17, 14, 0));
    }

    @Test
    void createOrder_validRequest_returns202WithOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.create(any(OrderRequest.class))).thenReturn(sampleOrder(orderId));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 42, "product": "laptop", "quantity": 3}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value(42))
                .andExpect(jsonPath("$.product").value("laptop"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.status").value("NEW"));

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
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.validationErrors.customerId").value("customerId is required"))
                .andExpect(jsonPath("$.validationErrors.product").value("product must not be blank"))
                .andExpect(jsonPath("$.validationErrors.quantity").value("quantity must be greater than 0"));

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
                .andExpect(jsonPath("$.validationErrors.quantity").exists());

        verify(orderService, never()).create(any());
    }

    @Test
    void createOrder_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrders_defaults_usesPage0Size20NewestFirst() throws Exception {
        when(orderService.findAll(eq(null), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(orderService).findAll(eq(null), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getOrders_withStatusPageSizeAndSort_passesThemToService() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.findAll(eq(OrderStatus.AVAILABLE), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(sampleOrder(id)), 1, 5, 153, 31));


        mockMvc.perform(get("/orders")
                        .param("page", "1").param("size", "5")
                        .param("status", "AVAILABLE").param("sort", "quantity,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(153))
                .andExpect(jsonPath("$.totalPages").value(31));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(orderService).findAll(eq(OrderStatus.AVAILABLE), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort().getOrderFor("quantity").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getOrders_sizeTooLarge_returns400() throws Exception {
        mockMvc.perform(get("/orders").param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.size").exists());

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrders_negativePage_returns400() throws Exception {
        mockMvc.perform(get("/orders").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.page").exists());
    }

    @Test
    void getOrders_unknownSortField_returns400() throws Exception {
        mockMvc.perform(get("/orders").param("sort", "password,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.sort").exists());

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrders_unknownStatus_returns400ListingAllowedValues() throws Exception {
        mockMvc.perform(get("/orders").param("status", "SHIPPED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value(
                        "Invalid value 'SHIPPED' for parameter 'status'. Allowed values: [NEW, AVAILABLE, OUT_OF_STOCK]"));
    }

    @Test
    void getOrders_nonNumericPage_returns400() throws Exception {
        mockMvc.perform(get("/orders").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
    }

    @Test
    void wrongHttpMethod_returns405() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/orders"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"));
    }
}