package com.pricer.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricer.config.PricingConfig;
import com.pricer.model.PriceCalculationResult;
import com.pricer.model.TickData;
import com.pricer.scheduler.PricingScheduler;
import com.pricer.service.PricingCalculationService;
import com.pricer.service.RedisTickDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo class to test the pricing engine with the sample Redis keys from the problem statement
 */
@Component
public class PricingEngineDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(PricingEngineDemo.class);
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private PricingScheduler pricingScheduler;
    
    @Autowired
    private PricingConfig pricingConfig;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void runDemo() {
        logger.info("=== PRICING ENGINE DEMO STARTED ===");
        
        // Load sample data as provided in the problem statement
        loadSampleRedisData();
        
        // Show configuration
        logConfiguration();
        
        // Run pricing calculations
        runPricingCalculations();
        
        logger.info("=== PRICING ENGINE DEMO COMPLETED ===");
    }
    
    /**
     * Load the sample Redis keys from the problem statement with mock tick data
     */
    private void loadSampleRedisData() {
        logger.info("Loading sample Redis data...");
        
        String[] sampleKeys = {
            "tickbuf:P2:USD.INR:SPOT:20000.0",
            "tickbuf:P1:USD.INR:FORWARD:10000.0", 
            "tickbuf:P3:USD.INR:FORWARD:30000.0",
            "tickbuf:P1:USD.INR:SPOT:1000000.0",
            "tickbuf:P3:USD.INR:SPOT:30000.0",
            "tickbuf:P1:USD.INR:FORWARD:30000.0",
            "tickbuf:P2:USD.INR:FORWARD:30000.0",
            "tickbuf:P2:USD.INR:SPOT:10000.0",
            "tickbuf:P1:USD.INR:FORWARD:20000.0",
            "tickbuf:P3:USD.INR:SPOT:10000.0",
            "tickbuf:P2:USD.INR:FORWARD:10000.0",
            "tickbuf:P3:USD.INR:FORWARD:1000000.0",
            "tickbuf:P1:USD.INR:SPOT:10000.0",
            "tickbuf:P2:USD.INR:SPOT:1000000.0",
            "tickbuf:P2:USD.INR:FORWARD:20000.0",
            "tickbuf:P1:USD.INR:FORWARD:1000000.0",
            "tickbuf:P2:USD.INR:SPOT:30000.0",
            "tickbuf:P3:USD.INR:FORWARD:20000.0",
            "tickbuf:P3:USD.INR:SPOT:1000000.0",
            "tickbuf:P3:USD.INR:FORWARD:10000.0",
            "tickbuf:P1:USD.INR:SPOT:20000.0",
            "tickbuf:P1:USD.INR:SPOT:30000.0",
            "tickbuf:P2:USD.INR:FORWARD:1000000.0",
            "tickbuf:P3:USD.INR:SPOT:20000.0"
        };
        
        for (String key : sampleKeys) {
            String tickJson = createMockTickDataJson(key);
            redisTemplate.opsForValue().set(key, tickJson);
        }
        
        logger.info("Loaded {} sample Redis keys", sampleKeys.length);
    }
    
    /**
     * Create mock tick data JSON for a given Redis key
     */
    private String createMockTickDataJson(String key) {
        try {
            // Parse key to extract metadata
            String[] parts = key.split(":");
            String source = parts[1];
            String type = parts[3];
            BigDecimal volume = new BigDecimal(parts[4]);
            
            // Generate realistic bid/ask rates based on source and volume
            BigDecimal baseRate = new BigDecimal("82.50"); // USD.INR base rate
            
            // Source-specific variations
            BigDecimal sourceSpread;
            switch (source) {
                case "P1":
                    sourceSpread = new BigDecimal("0.02"); // Tighter spreads for P1
                    break;
                case "P2":
                    sourceSpread = new BigDecimal("0.05"); // Medium spreads for P2
                    break;
                case "P3":
                    sourceSpread = new BigDecimal("0.10"); // Wider spreads for P3
                    break;
                default:
                    sourceSpread = new BigDecimal("0.05");
                    break;
            }
            
            // Volume-based adjustments
            BigDecimal volumeAdjustment = volume.compareTo(new BigDecimal("100000")) > 0 ? 
                    new BigDecimal("0.01") : new BigDecimal("0.03");
            
            // Type-based adjustments
            BigDecimal typeAdjustment = "FORWARD".equals(type) ? 
                    new BigDecimal("0.02") : BigDecimal.ZERO;
            
            BigDecimal bidRate = baseRate.subtract(sourceSpread).add(volumeAdjustment).add(typeAdjustment);
            BigDecimal askRate = baseRate.add(sourceSpread).add(volumeAdjustment).add(typeAdjustment);
            
            Map<String, Object> tickData = new HashMap<>();
            tickData.put("bidRate", bidRate.toString());
            tickData.put("askRate", askRate.toString());
            tickData.put("timestamp", LocalDateTime.now().toString());
            tickData.put("source", source);
            
            return objectMapper.writeValueAsString(tickData);
            
        } catch (Exception e) {
            logger.error("Error creating mock tick data for key {}: {}", key, e.getMessage());
            return "{\"bidRate\":\"82.50\",\"askRate\":\"82.55\",\"timestamp\":\"" + LocalDateTime.now() + "\"}";
        }
    }
    
    /**
     * Log current pricing configuration
     */
    private void logConfiguration() {
        logger.info("=== PRICING CONFIGURATION ===");
        pricingConfig.getSourceStrategies().forEach((source, strategy) -> {
            logger.info("  {} -> {}: {}", source, strategy, strategy.getDescription());
        });
    }
    
    /**
     * Run pricing calculations and log results
     */
    private void runPricingCalculations() {
        logger.info("=== RUNNING PRICING CALCULATIONS ===");
        
        try {
            List<PriceCalculationResult> results = pricingScheduler.calculatePricingManually();
            
            if (results.isEmpty()) {
                logger.warn("No pricing results generated!");
                return;
            }
            
            logDetailedResults(results);
            
        } catch (Exception e) {
            logger.error("Error during pricing calculations", e);
        }
    }
    
    /**
     * Log detailed pricing results in a formatted manner
     */
    private void logDetailedResults(List<PriceCalculationResult> results) {
        logger.info("=== DETAILED PRICING RESULTS ===");
        
        Map<String, Map<String, Map<String, PriceCalculationResult>>> groupedResults = new HashMap<>();
        
        // Group results by currency -> type -> source
        for (PriceCalculationResult result : results) {
            groupedResults
                .computeIfAbsent(result.getCurrency(), k -> new HashMap<>())
                .computeIfAbsent(result.getType(), k -> new HashMap<>())
                .put(result.getSource(), result);
        }
        
        // Log results in organized format
        groupedResults.forEach((currency, typeMap) -> {
            logger.info("Currency: {}", currency);
            
            typeMap.forEach((type, sourceMap) -> {
                logger.info("  Type: {}", type);
                
                sourceMap.forEach((source, result) -> {
                    logger.info("    {} [{}]: Vol={} | Bid={} | Ask={} | Strategy={}",
                        source, 
                        result.getStrategy(),
                        result.getVolume(),
                        result.getCalculatedBidRate(),
                        result.getCalculatedAskRate(),
                        result.getDescription());
                });
            });
        });
        
        // Summary statistics
        logger.info("=== SUMMARY ===");
        logger.info("Total calculations: {}", results.size());
        long p1Count = results.stream().filter(r -> "P1".equals(r.getSource())).count();
        long p2Count = results.stream().filter(r -> "P2".equals(r.getSource())).count();
        long p3Count = results.stream().filter(r -> "P3".equals(r.getSource())).count();
        
        logger.info("Results by source: P1={}, P2={}, P3={}", p1Count, p2Count, p3Count);
    }
}