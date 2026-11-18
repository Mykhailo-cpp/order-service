package com.example.order_service.controller;

import com.example.order_service.config.ApiErrors;
import com.example.order_service.config.ApiMessages;
import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.dto.PageResponse;
import com.example.order_service.model.OrderStatus;
import com.example.order_service.service.OrderService;
import com.example.order_service.util.PageRequestFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create an order",
            description = "Stores the order with status NEW and publishes ORDER_CREATED to Kafka. "
                    + "Status is updated asynchronously, hence 202 rather than 201.")
    @ApiResponse(responseCode = "202", description = ApiMessages.HTTP_202)
    @ApiErrors.BadRequest
    @ApiErrors.ServerError
    @PostMapping
    public ResponseEntity<OrderCreatedEvent> createOrder(@Valid @RequestBody OrderRequest request){
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(orderService.create(request));
    }

    @Operation(summary = "List orders",
            description = "Paginated, optionally filtered by status. Default sort: createdAt,desc. "
                    + "Sortable fields: createdAt, customerId, product, quantity, status.")
    @ApiResponse(responseCode = "200", description = ApiMessages.HTTP_200)
    @ApiErrors.BadRequest
    @ApiErrors.ServerError
    @GetMapping
    public PageResponse<OrderResponse> getOrders(
            @Parameter(description = "Zero-based page index")
            @RequestParam(name = "page", defaultValue = "" + PageRequestFactory.DEFAULT_PAGE) int page,

            @Parameter(description = "Page size (1-100)")
            @RequestParam(name = "size", defaultValue = "" + PageRequestFactory.DEFAULT_SIZE) int size,

            @Parameter(description = "Only return orders with this status")
            @RequestParam(name = "status", required = false) OrderStatus status,

            @Parameter(description = "Sort as <field>[,asc|desc]", example = "createdAt,desc")
            @RequestParam(name = "sort", required = false) String sort) {

        return orderService.findAll(status, PageRequestFactory.of(page, size, sort));
    }
}
