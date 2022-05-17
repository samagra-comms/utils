package com.uci.utils.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.utils.kafka.adapter.TextMapSetterAdapter;

import io.fusionauth.jwt.domain.Header;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotNull;

@Service
@Slf4j
public class RecordProducer {

    private final KafkaTemplate<String, String> producer;

    public RecordProducer(KafkaTemplate<String, String> producer) {
    	this.producer = producer;
    }
    
    public void send(String topic, String message, Context currentContext) {
    	List<RecordHeader> headers = Arrays.asList();
    	ProducerRecord<String, String> record = new ProducerRecord(topic, null, "", message, headers);
    	/* Propagate open telemetry current context by injecting it to kafka headers */
    	GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(currentContext, record.headers(), TextMapSetterAdapter.setter);
//    	log.info("headers:"+record.headers());
//    	Context extracted = GlobalOpenTelemetry.getPropagators().getTextMapPropagator().extract(Context.current(), record.headers(), getter);
//        log.info("extracted: "+extracted);
        
    	producer
                .send(record)
                .addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                    @Override
                    public void onFailure(@NotNull Throwable throwable) {
                        log.error("Unable to push {} to {} topic due to {}", message, topic, throwable.getMessage());
                    }

                    @Override
                    public void onSuccess(SendResult<String, String> stringStringSendResult) {
                        log.info("Pushed to topic {}", topic);
                    }
                });
    }
}