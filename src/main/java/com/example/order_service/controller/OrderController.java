package com.example.order_service.controller;

import com.example.order_service.config.ApiErrors;
import com.example.order_service.config.ApiMessages;
import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
