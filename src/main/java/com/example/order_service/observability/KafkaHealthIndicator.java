package com.example.order_service.observability;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class KafkaHealthIndicator implements HealthIndicator {
    private static final int TIMEOUT_MS = 2000;

    private final KafkaAdmin kafkaAdmin;

    @Override
    public Health health(){
        try(AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClusterResult clusterResult = admin.describeCluster(new DescribeClusterOptions().timeoutMs(TIMEOUT_MS));

            String clusterId = clusterResult.clusterId().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            int nodes = clusterResult.nodes().get(TIMEOUT_MS, TimeUnit.MILLISECONDS).size();

            return Health.up().withDetail("clusterId", clusterId).withDetail("nodes", nodes).build();
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            return Health.down(e).build();
        }catch (Exception e){
            return Health.down(e).build();
        }
    }
}
