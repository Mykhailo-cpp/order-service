package com.example.order_service.kafka;

import com.example.order_service.config.KafkaTopicConfig;
import com.example.order_service.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publish(OrderCreatedEvent event){
        kafkaTemplate.send(KafkaTopicConfig.ORDER_CREATED_TOPIC, event.orderId().toString(), event)
                .whenComplete((result, exception) -> {
                   if(exception != null){
                       log.error("Failed to publish order {}", event.orderId(), exception);
                   }
                   else{
                       log.info("Published order {} to {} (partition {}, offset {})",
                               event.orderId(),
                               result.getRecordMetadata().topic(),
                               result.getRecordMetadata().partition(),
                               result.getRecordMetadata().offset());
                   }
                });
    }

}
