package com.example.instructions.service;

import com.example.instructions.model.CanonicalTrade;
import com.example.instructions.model.PlatformTrade;
import com.example.instructions.util.TradeTransformer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MappingIterator;
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
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TradeService {

	private static final Logger log = LoggerFactory.getLogger(TradeService.class);
	
	private final String platformId;
	private final TradeTransformer tradeTransformer;
	private final KafkaPublisher kafkaPublisher;
	private final ObjectMapper objectMapper;
	private final CsvMapper csvMapper;

	private final ConcurrentHashMap<String, CanonicalTrade> auditStore = new ConcurrentHashMap<>();

	public TradeService(TradeTransformer tradeTransformer, KafkaPublisher kafkaPublisher, ObjectMapper objectMapper,
			CsvMapper csvMapper, @Value("${trade.platform.id:ACCT123}") String platformId) {
		this.tradeTransformer = tradeTransformer;
		this.kafkaPublisher = kafkaPublisher;
		this.objectMapper = objectMapper;
		this.csvMapper = csvMapper;
		this.platformId = platformId;
	}

	public int processFileUpload(MultipartFile file) {
		String fileName = file.getOriginalFilename();

		if (file.isEmpty() || fileName == null) {
			throw new IllegalArgumentException("File cannot be empty.");
		}

		int processedCount = 0;

		try (InputStream inputStream = file.getInputStream()) {

			if (fileName.endsWith(".csv")) {
				processedCount = processCsv(inputStream);
			} else if (fileName.endsWith(".json")) {
				processedCount = processJson(inputStream);
			} else {
				throw new IllegalArgumentException("Unsupported file type. Only .csv and .json are accepted.");
			}

		} catch (IOException e) {
			log.error("I/O error during file stream processing.", e);
			throw new RuntimeException("Could not read file stream.", e);
		}

		log.info("Successfully processed {} records from file: {}", processedCount, fileName);
		return processedCount; // Return the count to the controller
	}

	private int processJson(InputStream inputStream) throws IOException {
		List<CanonicalTrade> trades = objectMapper.readerForListOf(CanonicalTrade.class).readValue(inputStream);

		int count = 0;
		for (CanonicalTrade trade : trades) {
			processTrade(trade);
			count++;
		}

		return count;
	}

	private int processCsv(InputStream inputStream) throws IOException {
		CsvSchema schema = CsvSchema.emptySchema().withHeader();
		MappingIterator<CanonicalTrade> it = csvMapper.readerFor(CanonicalTrade.class).with(schema)
				.readValues(inputStream);

		int count = 0;
		while (it.hasNext()) {
			CanonicalTrade trade = it.next();
			processTrade(trade);
			count++;
		}

		return count;
	}

	public void processTrade(CanonicalTrade canonicalTrade) {
		String originalAccountNumber = canonicalTrade != null ? canonicalTrade.getAccountNumber() : null;

		if (originalAccountNumber == null || originalAccountNumber.isEmpty()) {
			log.error("Validation Failed: CanonicalTrade or AccountNumber is null/empty. Dropping message.");
			return;
		}

		try {
			// 1. AUDIT (In-Memory Storage)
			auditStore.put(originalAccountNumber, canonicalTrade);

			// 2. TRANSFORM and SANITIZE
			PlatformTrade transformedTrade = tradeTransformer.transformToPlatformTrade(canonicalTrade);

			// 3. WRAPPER CREATION (Correctly uses the AllArgsConstructor)
			PlatformTrade.PlatformTradeWrapper wrapper = new PlatformTrade.PlatformTradeWrapper(this.platformId,
					transformedTrade);

			// 4. SERIALIZE
			String finalTradeJson = objectMapper.writeValueAsString(wrapper);

			// 5. LOG (Required step for verification)
			log.info("PLATFORM-SPECIFIC JSON: {}", finalTradeJson);

			// 6. PUBLISH TO KAFKA
			kafkaPublisher.publish(wrapper);

		} catch (IllegalArgumentException e) {
			log.error("Validation/Sanitization Failed for CanonicalTrade with Account {}. Dropping message: {}",
					originalAccountNumber, e.getMessage());

		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			log.error("Error serializing PlatformTrade for Account {}: {}", originalAccountNumber, e.getMessage());

		} catch (Exception e) {
			log.error("Unexpected error during trade processing for Account {}.", originalAccountNumber, e);
		}
	}
}