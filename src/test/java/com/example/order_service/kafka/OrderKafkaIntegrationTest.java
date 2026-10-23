package com.example.order_service.kafka;

import com.example.order_service.config.KafkaTopicConfig;
import com.example.order_service.dto.OrderCreatedEvent;
import com.example.order_service.dto.OrderRequest;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderStatus;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.service.OrderService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {KafkaTopicConfig.ORDER_CREATED_TOPIC, KafkaTopicConfig.INVENTORY_RESULT_TOPIC},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class OrderKafkaIntegrationTest {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void createOrder_publishesOrderCreatedEventToKafka() {
        try (KafkaConsumer<String, String> consumer = newConsumer()) {
            consumer.subscribe(List.of(KafkaTopicConfig.ORDER_CREATED_TOPIC));
            consumer.poll(Duration.ofMillis(500));

            OrderCreatedEvent event = orderService.create(new OrderRequest(5L, "tablet", 2));

            ConsumerRecord<String, String> record = pollFor(consumer, event.orderId().toString());
            assertThat(record.value())
                    .contains(event.orderId().toString())
                    .contains("\"product\":\"tablet\"")
                    .contains("\"quantity\":2");
        }
    }

    @Test
    void inventoryResultMessage_updatesOrderStatusInDatabase() {
        OrderCreatedEvent event = orderService.create(new OrderRequest(9L, "camera", 1));
        UUID orderId = event.orderId();
        assertThat(orderRepository.findById(orderId)).get()
                .extracting(Order::getStatus).isEqualTo(OrderStatus.NEW);

        sendRaw(KafkaTopicConfig.INVENTORY_RESULT_TOPIC, orderId.toString(),
                "{\"orderId\":\"" + orderId + "\",\"status\":\"OUT_OF_STOCK\"}");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(orderRepository.findById(orderId)).get()
                        .extracting(Order::getStatus).isEqualTo(OrderStatus.OUT_OF_STOCK));
    }

    @Test
    void inventoryResultForUnknownOrder_isIgnoredAndListenerKeepsWorking() {
        UUID unknown = UUID.randomUUID();
        sendRaw(KafkaTopicConfig.INVENTORY_RESULT_TOPIC, unknown.toString(),
                "{\"orderId\":\"" + unknown + "\",\"status\":\"AVAILABLE\"}");

        UUID orderId = orderService.create(new OrderRequest(3L, "speaker", 1)).orderId();
        sendRaw(KafkaTopicConfig.INVENTORY_RESULT_TOPIC, orderId.toString(),
                "{\"orderId\":\"" + orderId + "\",\"status\":\"AVAILABLE\"}");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(orderRepository.findById(orderId)).get()
                        .extracting(Order::getStatus).isEqualTo(OrderStatus.AVAILABLE));
    }

    private void sendRaw(String topic, String key, String json) {
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            producer.send(new ProducerRecord<>(topic, key, json));
            producer.flush();
        }
    }

    private KafkaConsumer<String, String> newConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
    }

    private ConsumerRecord<String, String> pollFor(KafkaConsumer<String, String> consumer, String key) {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            for (ConsumerRecord<String, String> r : consumer.poll(Duration.ofMillis(500))) {
                if (key.equals(r.key())) {
                    return r;
                }
            }
        }
        throw new AssertionError("No record with key " + key + " received within 15s");
    }
}