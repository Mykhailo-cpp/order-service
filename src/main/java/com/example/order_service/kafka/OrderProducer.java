package com.example.order_service.kafka;

import com.example.order_service.config.KafkaTopicConfig;
import com.example.order_service.dto.OrderCancelledEvent;
import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.dto.OrderUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCreated(OrderCreatedEvent event){
        String key = event.customerId().toString();
        send(key, event, event.orderId().toString());
    }

    public void publishUpdated(OrderUpdatedEvent event, Long customerId){
        String key = String.valueOf(customerId);
        send(key, event, event.orderId().toString());
    }

    public void publishCancelled(OrderCancelledEvent event, Long customerId){
        String key = String.valueOf(customerId);
        send(key, event, event.orderId().toString());
    }

    private void send(String key, Object event, String orderId){
        kafkaTemplate.send(KafkaTopicConfig.ORDER_EVENTS_TOPIC, key, event)
                .whenComplete((result, exception) -> {
                   if(exception != null){
                       log.error("Failed to publish order {}", orderId, exception);
                   }
                   else{
                       log.info("Published order {} to {} (partition {}, offset {})",
                               orderId,
                               result.getRecordMetadata().topic(),
                               result.getRecordMetadata().partition(),
                               result.getRecordMetadata().offset());
                   }
                });
    }

}
