package com.example.instructions.service;

import com.example.instructions.model.CanonicalTrade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
 
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Service
public class KafkaListenerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaListenerService.class);
    private final TradeService tradeService; 

    public KafkaListenerService(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @KafkaListener(topics = "${instructions.inbound.topic}", 
                   groupId = "${spring.kafka.consumer.group-id}")
    public void consumeInboundInstructions(
        CanonicalTrade canonicalTrade,
        ConsumerRecord<?, ?> record, 
        Consumer<?, ?> consumer) {  
        if (canonicalTrade == null) {
            log.warn("Received null or unparseable Kafka message. Skipping.");
            return;
        }

        log.info("Kafka Listener: Received Trade for Security ID: {}", canonicalTrade.getSecurityId());        
        tradeService.processTrade(canonicalTrade); 
    }
}