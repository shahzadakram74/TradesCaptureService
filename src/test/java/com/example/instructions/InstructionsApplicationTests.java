package com.example.instructions;

import com.example.instructions.model.CanonicalTrade;
import com.example.instructions.service.TradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.context.annotation.Lazy; 
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate; 
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.apache.kafka.clients.producer.ProducerConfig; 
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer; 
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory; // <-- Crucial for Listener Activation
import org.apache.kafka.common.serialization.StringDeserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; 

@ActiveProfiles("local")
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"instructions.inbound"} 
)
@TestPropertySource(properties = {
    "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.group-id=test-group-id-${random.uuid}",
    "spring.kafka.listener.poll-timeout=100",
    "spring.kafka.listener.idle-event-interval=100",
    "spring.kafka.consumer.auto-offset-reset=earliest" 
})
class InstructionsApplicationTests {

    @Autowired
    private KafkaTemplate<String, CanonicalTrade> kafkaTemplate;

    @Autowired
    private TradeService tradeServiceSpy; 
    
    private final String INBOUND_TOPIC = "instructions.inbound"; 
    
    @Configuration
    static class TestConfig {
        
        
        @Bean
        @Primary
        public TradeService tradeServiceSpy(@Lazy TradeService realTradeService) {
            return Mockito.spy(realTradeService);
        }

        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule()); 
            return mapper;
        }

        @Bean
        public ProducerFactory<String, CanonicalTrade> producerFactory(
                @Value("${spring.kafka.producer.bootstrap-servers}") String bootstrapServers,
                ObjectMapper objectMapper) { 
            
            Map<String, Object> props = new HashMap<>();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false); 
            
            return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JsonSerializer<>(objectMapper)
            );
        }
        
        @Bean
        public KafkaTemplate<String, CanonicalTrade> kafkaTemplate(
                ProducerFactory<String, CanonicalTrade> producerFactory) {
            
            return new KafkaTemplate<>(producerFactory);
        }
        
        @Bean
        public ConsumerFactory<String, CanonicalTrade> consumerFactory(
                @Value("${spring.kafka.consumer.bootstrap-servers}") String bootstrapServers) {

            Map<String, Object> props = new HashMap<>();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            
            props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.instructions.model");
            props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CanonicalTrade.class.getName());
            
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-id-default");
            
            return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(CanonicalTrade.class)
            );
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, CanonicalTrade> kafkaListenerContainerFactory(
                ConsumerFactory<String, CanonicalTrade> consumerFactory,
                @Value("${spring.kafka.listener.poll-timeout}") Long pollTimeout) { 
            
            ConcurrentKafkaListenerContainerFactory<String, CanonicalTrade> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
            
            factory.setConsumerFactory(consumerFactory);
            factory.getContainerProperties().setPollTimeout(pollTimeout);
            
            return factory;
        }
    }

    @Test
    void testMessageFlow_ShouldBeProcessedByConsumer() {
        CanonicalTrade testTrade = new CanonicalTrade(
            "9876543210",               
            "TEST_SIMPLE_SEC",          
            "B",                        
            100L,                       
            new BigDecimal("100.00"),   
            new BigDecimal("10000.00"), 
            LocalDateTime.now()         
        );        
        kafkaTemplate.send(INBOUND_TOPIC, "SIMPLE-KEY", testTrade);
        verify(tradeServiceSpy, timeout(10000).times(1)).processTrade(testTrade);
    }
}