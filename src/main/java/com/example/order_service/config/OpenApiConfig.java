package com.example.order_service.config;

import com.example.order_service.dto.InventoryResultEvent;
import com.example.order_service.dto.OrderCancelledEvent;
import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.dto.OrderUpdatedEvent;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String KAFKA_DOCS = """
            ## Kafka events

            | Direction | Topic | Key | `__TypeId__` header | Payload schema |
            |---|---|---|---|---|
            | Produced | `order-events` (3 partitions) | `customerId` | `ORDER_CREATED` | `OrderCreatedEvent` |
            | Produced | `order-events` | `customerId` | `ORDER_UPDATED` | `OrderUpdatedEvent` |
            | Produced | `order-events` | `customerId` | `ORDER_CANCELLED` | `OrderCancelledEvent` |
            | Consumed | `inventory-result` | `orderId` | - | `InventoryResultEvent` |

            Keying by `customerId` keeps all events of one customer in order on the same partition.
            Every message carries an `X-Correlation-Id` header (propagated from the HTTP request).
            Timestamps use the format `yyyy-MM-dd'T'HH:mm:ss.SSS`.
            Event payload schemas are listed under **Schemas** below.
            """;

    private static final List<Class<?>> EVENT_TYPES = List.of(
            OrderCreatedEvent.class,
            OrderUpdatedEvent.class,
            OrderCancelledEvent.class,
            InventoryResultEvent.class);

    @Bean
    public OpenAPI orderServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Order Service API")
                .version("v1")
                .description("REST API for creating and querying orders.\n\n" + KAFKA_DOCS));
    }

    @Bean
    public OpenApiCustomizer kafkaEventSchemasCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            for (Class<?> type : EVENT_TYPES) {
                ModelConverters.getInstance().readAll(type)
                        .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
            }
        };
    }
}