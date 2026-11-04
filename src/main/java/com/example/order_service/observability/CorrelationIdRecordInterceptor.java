package com.example.order_service.observability;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CorrelationIdRecordInterceptor implements RecordInterceptor<Object, Object> {

    @Override
    public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record,
                                                    Consumer<Object, Object> consumer){

        Header header = record.headers().lastHeader(Correlation.HEADER);

        String raw = header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
        MDC.put(Correlation.MDC_KEY, Correlation.validOrNew(raw));

        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer){
        MDC.remove(Correlation.MDC_KEY);
    }
}
