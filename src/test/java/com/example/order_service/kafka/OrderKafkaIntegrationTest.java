package com.example.order_service.kafka;

import com.example.order_service.config.KafkaTopicConfig;
import com.example.order_service.model.Order;
import com.example.order_service.model.OrderStatus;
import com.example.order_service.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class OrderKafkaIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void postOrders_publishesOrderCreatedEventToKafka() throws Exception {
        HttpResponse<String> response = postOrder("""
                {"customerId":42,"product":"tablet","quantity":2}""");

        assertThat(response.statusCode()).isEqualTo(202);
        String orderId = MAPPER.readTree(response.body()).get("orderId").asText();

        try (KafkaConsumer<String, String> consumer = newConsumer(KafkaTopicConfig.ORDER_EVENTS_TOPIC)) {
            ConsumerRecord<String, String> record =
                    pollFor(consumer, r -> r.value().contains(orderId), Duration.ofSeconds(15));

            assertThat(record).as("event for order %s on %s", orderId, KafkaTopicConfig.ORDER_EVENTS_TOPIC)
                    .isNotNull();

            assertThat(record.key()).isEqualTo("42");

            assertThat(new String(record.headers().lastHeader("__TypeId__").value()))
                    .isEqualTo("ORDER_CREATED");

            JsonNode body = MAPPER.readTree(record.value());
            assertThat(body.get("orderId").asText()).isEqualTo(orderId);
            assertThat(body.get("customerId").asLong()).isEqualTo(42L);
            assertThat(body.get("product").asText()).isEqualTo("tablet");
            assertThat(body.get("quantity").asInt()).isEqualTo(2);
        }

        assertThat(orderRepository.findById(UUID.fromString(orderId)))
                .get().extracting(Order::getStatus).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void postInvalidOrder_isRejectedAndNothingIsPublished() throws Exception {
        HttpResponse<String> response = postOrder("""
                {"customerId":777,"product":"ghost","quantity":-1}""");
        assertThat(response.statusCode()).isEqualTo(400);

        try (KafkaConsumer<String, String> consumer = newConsumer(KafkaTopicConfig.ORDER_EVENTS_TOPIC)) {
            ConsumerRecord<String, String> record =
                    pollFor(consumer, r -> "777".equals(r.key()), Duration.ofSeconds(3));
            assertThat(record).as("no event may be published for a rejected request").isNull();
        }
    }

    @Test
    void inventoryResultMessage_updatesOrderStatusInDatabase() throws Exception {
        HttpResponse<String> response = postOrder("""
                {"customerId":9,"product":"camera","quantity":1}""");
        UUID orderId = UUID.fromString(MAPPER.readTree(response.body()).get("orderId").asText());

        send(KafkaTopicConfig.INVENTORY_RESULT_TOPIC, orderId.toString(),
                "{\"orderId\":\"" + orderId + "\",\"status\":\"OUT_OF_STOCK\"}");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(orderRepository.findById(orderId))
                        .get().extracting(Order::getStatus).isEqualTo(OrderStatus.OUT_OF_STOCK));
    }

    @Test
    void inventoryResultForUnknownOrder_isIgnoredAndListenerKeepsWorking() throws Exception {
        send(KafkaTopicConfig.INVENTORY_RESULT_TOPIC, "ghost",
                "{\"orderId\":\"" + UUID.randomUUID() + "\",\"status\":\"AVAILABLE\"}");

        HttpResponse<String> response = postOrder("""
                {"customerId":10,"product":"phone","quantity":1}""");
        UUID orderId = UUID.fromString(MAPPER.readTree(response.body()).get("orderId").asText());
        send(KafkaTopicConfig.INVENTORY_RESULT_TOPIC, orderId.toString(),
                "{\"orderId\":\"" + orderId + "\",\"status\":\"AVAILABLE\"}");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(orderRepository.findById(orderId))
                        .get().extracting(Order::getStatus).isEqualTo(OrderStatus.AVAILABLE));
    }

    private HttpResponse<String> postOrder(String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/orders"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void send(String topic, String key, String value) {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(topic, key, value));
            producer.flush();
        }
    }

    private KafkaConsumer<String, String> newConsumer(String topic) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(topic));
        return consumer;
    }

    private ConsumerRecord<String, String> pollFor(KafkaConsumer<String, String> consumer,
                                                   Predicate<ConsumerRecord<String, String>> match,
                                                   Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            for (ConsumerRecord<String, String> r : consumer.poll(Duration.ofMillis(300))) {
                if (match.test(r)) {
                    return r;
                }
            }
        }
        return null;
    }
}