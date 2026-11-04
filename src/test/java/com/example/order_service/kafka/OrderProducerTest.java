package com.example.order_service.kafka;

import com.example.order_service.config.KafkaTopicConfig;
import com.example.order_service.dto.OrderCancelledEvent;
import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.dto.OrderUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderProducerTest {

    private static final String TOPIC = KafkaTopicConfig.ORDER_EVENTS_TOPIC;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderProducer orderProducer;

    private final UUID orderId = UUID.randomUUID();

    @Test
    void publishCreated_sendsToOrderEventsTopicKeyedByCustomerId() {
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, 7L, "monitor", 1, LocalDateTime.now());
        when(kafkaTemplate.send(TOPIC, "7", event)).thenReturn(new CompletableFuture<>());

        orderProducer.publishCreated(event);

        verify(kafkaTemplate).send(TOPIC, "7", event);
    }

    @Test
    void publishUpdated_sendsToOrderEventsTopicKeyedByCustomerId() {
        OrderUpdatedEvent event = new OrderUpdatedEvent(orderId, "monitor", 3, LocalDateTime.now());
        when(kafkaTemplate.send(TOPIC, "7", event)).thenReturn(new CompletableFuture<>());

        orderProducer.publishUpdated(event, 7L);

        verify(kafkaTemplate).send(TOPIC, "7", event);
    }

    @Test
    void publishCancelled_sendsToOrderEventsTopicKeyedByCustomerId() {
        OrderCancelledEvent event = new OrderCancelledEvent(orderId, "changed my mind", LocalDateTime.now());
        when(kafkaTemplate.send(TOPIC, "7", event)).thenReturn(new CompletableFuture<>());

        orderProducer.publishCancelled(event, 7L);

        verify(kafkaTemplate).send(TOPIC, "7", event);
    }

    @Test
    void publishCreated_whenBrokerSendFails_doesNotThrow() {
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, 7L, "monitor", 1, LocalDateTime.now());
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(TOPIC, "7", event)).thenReturn(failed);

        assertThatCode(() -> orderProducer.publishCreated(event)).doesNotThrowAnyException();
    }
}