package com.example.instructions.model;

import lombok.Data;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PlatformTrade {
    
    private String account; 
    private String security;
    private String type;
    private BigDecimal amount;
    private LocalDateTime timestamp;
         
    @Data
    @AllArgsConstructor
    public static class PlatformTradeWrapper {
        private String platform_id;
        private PlatformTrade trade;
    }
}