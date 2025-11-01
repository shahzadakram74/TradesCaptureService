package com.example.instructions.service;

import com.example.instructions.model.CanonicalTrade;
import com.example.instructions.model.PlatformTrade;
import com.example.instructions.model.PlatformTrade.PlatformTradeWrapper;
import com.example.instructions.util.TradeTransformer;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TradeService {

    private static final Logger log = LoggerFactory.getLogger(TradeService.class);
    
    private final TradeTransformer transformer;
    private final KafkaPublisher kafkaPublisher;
    private final TradeStore tradeStore;
    private final ObjectMapper objectMapper;
    private final CsvMapper csvMapper;
    
    @Value("${app.platform-id}")
    private String platformId;

    public TradeService(TradeTransformer transformer, KafkaPublisher kafkaPublisher, TradeStore tradeStore, ObjectMapper objectMapper) {
        this.transformer = transformer;
        this.kafkaPublisher = kafkaPublisher;
        this.tradeStore = tradeStore;
        this.objectMapper = objectMapper;
        this.csvMapper = new CsvMapper();
    }
   
    public void processTrade(CanonicalTrade canonicalTrade) {
        try {
            // 1. Store canonical trade (In-Memory Storage for Auditing)
            String canonicalId = tradeStore.store(canonicalTrade);
            
            // 2. Apply transformation and masking
            PlatformTrade platformTrade = transformer.transformToPlatformTrade(canonicalTrade);
            
            // 3. MANUAL CONFIRMATION: Log the Transformed JSON Output (for your audit)
            try {
                String finalJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(platformTrade);
                log.info("FINAL PLATFORM-SPECIFIC JSON READY FOR PUBLISHING:\n{}", finalJson);
            } catch (Exception e) {
                log.error("Error logging final JSON output.", e);
            }
            
            // 3a. Create the platform-specific JSON wrapper
            PlatformTradeWrapper tradeWrapper = new PlatformTradeWrapper(platformId, platformTrade);    
            
            // 4. Publish asynchronously to Kafka
            kafkaPublisher.publish(tradeWrapper);
                        
            log.debug("Trade with Canonical ID {} processed and published.", canonicalId);

        } catch (IllegalArgumentException e) {            
            log.error("Transformation validation failed for trade: {}. Error: {}", canonicalTrade.getCanonicalId(), e.getMessage());
        } catch (Exception e) {
            log.error("An unexpected error occurred during trade processing: {}", e.getMessage(), e);
        }
    }
   
    public int processFileUpload(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        AtomicInteger count = new AtomicInteger(0);

        try (InputStream inputStream = file.getInputStream()) {
            if (filename != null && filename.toLowerCase().endsWith(".csv")) {
                count.set(processCsv(inputStream));
            } else if (filename != null && filename.toLowerCase().endsWith(".json")) {
                count.set(processJson(inputStream));
            } else {
                throw new IllegalArgumentException("Unsupported file type. Must be .csv or .json.");
            }
        }
        return count.get();
    }
    
    private int processCsv(InputStream inputStream) throws IOException {
        int count = 0;
    
        CsvSchema schema = CsvSchema.emptySchema().withHeader();        
        MappingIterator<CanonicalTrade> it = csvMapper
            .readerFor(CanonicalTrade.class)
            .with(schema)
            .readValues(inputStream);

        // Stream-based processing for large files
        while (it.hasNext()) {
            CanonicalTrade trade = it.next();
            processTrade(trade);
            count++;
        }
        log.info("Finished processing {} trades from CSV file.", count);
        return count;
    }

    private int processJson(InputStream inputStream) throws IOException {       
        List<CanonicalTrade> trades = objectMapper.readValue(
            inputStream, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, CanonicalTrade.class)
        );         
        trades.forEach(this::processTrade);        
        log.info("Finished processing {} trades from JSON file.", trades.size());
        return trades.size();
    }
}