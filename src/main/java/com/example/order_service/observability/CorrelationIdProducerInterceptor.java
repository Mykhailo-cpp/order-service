package com.example.order_service.observability;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class CorrelationIdProducerInterceptor implements ProducerInterceptor<Object, Object> {

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record){
        String id = MDC.get(Correlation.MDC_KEY);

        if(id != null && record.headers().lastHeader(Correlation.HEADER) == null){
            record.headers().add(Correlation.HEADER, id.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception){

    }

    @Override
    public void close(){

    }

    @Override
    public void configure(Map<String, ?> configs){

    }
}
