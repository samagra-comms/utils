package com.uci.utils.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import jakarta.validation.constraints.NotNull;

@Service
@Slf4j
public class SimpleProducer {

    private final KafkaTemplate<String, String> simpleProducer;

    public SimpleProducer(KafkaTemplate<String, String> simpleProducer1) {
        this.simpleProducer = simpleProducer1;
    }

    public void send(String topic, String message) {
        simpleProducer
                .send(topic, message);
    }
}