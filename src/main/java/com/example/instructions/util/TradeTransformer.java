package com.example.instructions.util;

import com.example.instructions.model.CanonicalTrade;
import com.example.instructions.model.PlatformTrade;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@Slf4j
public class TradeTransformer {

    private static final Pattern SECURITY_ID_PATTERN = Pattern.compile("^[A-Z0-9]{3,}$");
   
    public PlatformTrade transformToPlatformTrade(CanonicalTrade canonicalTrade) {
        // 1. Mask account_number
        String maskedAccount = maskAccountNumber(canonicalTrade.getAccountNumber());

        // 2. validate format
        String validatedSecurityId = validateAndFormatSecurityId(canonicalTrade.getSecurityId());
        
        // 3. Normalize trade_type
        String normalizedTradeType = normalizeTradeType(canonicalTrade.getTradeType());

        return new PlatformTrade(
            maskedAccount,
            validatedSecurityId,
            normalizedTradeType,
            canonicalTrade.getAmount(),
            canonicalTrade.getTimestamp()
        );
    }
 
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            log.warn("Account number is too short or null. Masking fully.");
            return "****"; 
        }
        // Mask all but the last 4 digits
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
    
    private String validateAndFormatSecurityId(String securityId) {
        if (securityId == null || securityId.isBlank()) {
             throw new IllegalArgumentException("Security ID cannot be empty.");
        }
        String upperCaseId = securityId.toUpperCase();
        if (!SECURITY_ID_PATTERN.matcher(upperCaseId).matches()) {             
             log.warn("Security ID format validation warning for: {}", upperCaseId);             
        }
        return upperCaseId;
    }
  
    private String normalizeTradeType(String tradeType) {
        if (tradeType == null) return "U"; // Unknown
        String normalized = tradeType.toUpperCase().trim();
        return switch (normalized) {
            case "BUY", "B" -> "B";
            case "SELL", "S" -> "S";
            default -> {
                log.warn("Unknown trade type encountered: {}", tradeType);
                yield "U";
            }
        };
    }
}