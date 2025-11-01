package com.example.instructions.service;

import com.example.instructions.model.PlatformTrade.PlatformTradeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.outbound}")
    private String outboundTopic;

    public KafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void publish(PlatformTradeWrapper tradeWrapper) {
        String key = tradeWrapper.getTrade().getSecurity();               
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(outboundTopic, key, tradeWrapper);        
        future.whenComplete((result, ex) -> {
            if (ex == null) {                
                log.info("Successfully published trade with key '{}' to topic {}. Offset: {}", 
                         key, outboundTopic, result.getRecordMetadata().offset());
            } else {                
                log.error("Failed to publish trade with key '{}' to Kafka: {}", key, ex.getMessage());
            }
        });
    }
}