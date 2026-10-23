package com.example.order_service.kafka;

import com.example.order_service.config.KafkaTopicConfig;
import com.example.order_service.dto.OrderCreatedEvent;
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

    @Mock
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @InjectMocks
    private OrderProducer orderProducer;

    private final OrderCreatedEvent event =
            new OrderCreatedEvent(UUID.randomUUID(), 1L, "monitor", 1, LocalDateTime.now());

    @Test
    void publish_sendsEventToOrderCreatedTopicKeyedByOrderId() {
        when(kafkaTemplate.send(KafkaTopicConfig.ORDER_CREATED_TOPIC, event.orderId().toString(), event))
                .thenReturn(new CompletableFuture<>());

        orderProducer.publish(event);

        verify(kafkaTemplate).send(KafkaTopicConfig.ORDER_CREATED_TOPIC, event.orderId().toString(), event);
    }

    @Test
    void publish_whenBrokerSendFails_doesNotThrow() {
        CompletableFuture<SendResult<String, OrderCreatedEvent>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(KafkaTopicConfig.ORDER_CREATED_TOPIC, event.orderId().toString(), event))
                .thenReturn(failed);

        assertThatCode(() -> orderProducer.publish(event)).doesNotThrowAnyException();
    }
}