package com.example.order_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_CREATED_TOPIC = "order-created";
    public static final String INVENTORY_RESULT_TOPIC = "inventory-result";

    @Bean
    public NewTopic orderCreatedTopic(){
        return TopicBuilder.name(ORDER_CREATED_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryResultTopic(){
        return TopicBuilder.name(INVENTORY_RESULT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
