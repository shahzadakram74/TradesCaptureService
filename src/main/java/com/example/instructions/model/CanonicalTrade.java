package com.example.instructions.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalTrade {
    
    @JsonProperty("account_number")
    private String accountNumber; 

    @JsonProperty("security_id")
    private String securityId;

    @JsonProperty("trade_type")
    private String tradeType; 

    @JsonProperty("quantity")
    private Long quantity;
    
    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
        
    private String canonicalId;

	public CanonicalTrade(String accountNumber, String securityId, String tradeType, Long quantity, BigDecimal price,
			BigDecimal amount, LocalDateTime timestamp) {
		super();
		this.accountNumber = accountNumber;
		this.securityId = securityId;
		this.tradeType = tradeType;
		this.quantity = quantity;
		this.price = price;
		this.amount = amount;
		this.timestamp = timestamp;
	}    
}