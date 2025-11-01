package com.example.instructions.service;

import com.example.instructions.model.CanonicalTrade;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TradeStore {
    
    private final ConcurrentHashMap<String, CanonicalTrade> canonicalTradeStore = new ConcurrentHashMap<>();
    
    public String store(CanonicalTrade trade) {        
        String id = UUID.randomUUID().toString();
        trade.setCanonicalId(id);
        log.info("Received and storing new Trade in ConcurrentHashMap");
        canonicalTradeStore.put(id, trade);
        return id;
    }
  
    public CanonicalTrade get(String id) {
        return canonicalTradeStore.get(id);
    }    
    
}