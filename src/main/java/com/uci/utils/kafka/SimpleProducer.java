package com.uci.utils.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.validation.constraints.NotNull;

@Service
@Slf4j
public class SimpleProducer {

    private final KafkaTemplate<String, String> simpleProducer;

    public SimpleProducer(KafkaTemplate<String, String> simpleProducer1) {
        this.simpleProducer = simpleProducer1;
    }

    public void send(String topic, String message) {
        simpleProducer
                .send(topic, message)
                .addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                    @Override
                    public void onFailure(@NotNull Throwable throwable) {
                        log.error("Kafka::Send:Exception: Unable to push topic {} message {} due to {}", topic, message, throwable.getMessage());
                    }

                    @Override
                    public void onSuccess(SendResult<String, String> stringStringSendResult) {
                        log.info("Kafka::Send: Pushed to topic {}", topic);
                    }
                });
    }
}