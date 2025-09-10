package com.pricer.scheduler;

import com.pricer.config.PricingConfig;
import com.pricer.model.PriceCalculationResult;
import com.pricer.model.PricingStrategy;
import com.pricer.model.TickData;
import com.pricer.service.PricingCalculationService;
import com.pricer.service.RedisTickDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PricingScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(PricingScheduler.class);
    
    @Autowired
    private RedisTickDataService redisTickDataService;
    
    @Autowired
    private PricingCalculationService pricingCalculationService;
    
    @Autowired
    private PricingConfig pricingConfig;

    /**
     * Main scheduler method that runs periodically to process pricing
     * Runs every 30 seconds (can be configured)
     */
    @Scheduled(fixedRate = 30000) // 30 seconds
    public void processPricing() {
        logger.info("Starting pricing calculation cycle...");
        
        try {
            // 1. Retrieve all tick data from Redis
            List<TickData> allTickData = redisTickDataService.getAllTickData();
            
            if (allTickData.isEmpty()) {
                logger.warn("No tick data available for processing");
                return;
            }
            
            // 2. Group tick data by currency, type, and volume
            Map<String, List<TickData>> groupedTickData = redisTickDataService.groupTickData(allTickData);
            
            // 3. Process each group
            List<PriceCalculationResult> results = new ArrayList<>();
            
            for (Map.Entry<String, List<TickData>> entry : groupedTickData.entrySet()) {
                String groupKey = entry.getKey();
                List<TickData> ticks = entry.getValue();
                
                logger.debug("Processing group: {} with {} ticks", groupKey, ticks.size());
                
                // Process by source priority
                Map<String, List<TickData>> ticksBySource = ticks.stream()
                        .collect(Collectors.groupingBy(TickData::getSource));
                
                for (String source : Arrays.asList("P1", "P2", "P3")) {
                    List<TickData> sourceTicks = ticksBySource.get(source);
                    
                    if (sourceTicks != null && !sourceTicks.isEmpty()) {
                        PriceCalculationResult result = processSourceTicks(source, sourceTicks, groupKey);
                        if (result != null) {
                            results.add(result);
                        }
                    }
                }
            }
            
            // 4. Log and store results
            logPricingResults(results);
            
        } catch (Exception e) {
            logger.error("Error during pricing calculation cycle", e);
        }
    }
    
    /**
     * Process ticks for a specific source using its configured strategy
     */
    private PriceCalculationResult processSourceTicks(String source, List<TickData> sourceTicks, String groupKey) {
        try {
            // Get the pricing strategy for this source
            PricingStrategy strategy = pricingConfig.getStrategyForSource(source);
            
            // Extract currency, type, and volume from group key
            String[] parts = groupKey.split(":");
            if (parts.length != 3) {
                logger.error("Invalid group key format: {}", groupKey);
                return null;
            }
            
            String currency = parts[0];
            String type = parts[1];
            String volume = parts[2];
            
            // Calculate price using the strategy
            PriceCalculationResult result = pricingCalculationService.calculatePrice(
                    sourceTicks, strategy, currency, type, 
                    new java.math.BigDecimal(volume)
            );
            
            if (result != null) {
                result.setSource(source);
                logger.debug("Calculated {} for source {} - Bid: {}, Ask: {}", 
                        strategy, source, result.getCalculatedBidRate(), result.getCalculatedAskRate());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error processing ticks for source {}: {}", source, e.getMessage());
            return null;
        }
    }
    
    /**
     * Log pricing results in a structured format
     */
    private void logPricingResults(List<PriceCalculationResult> results) {
        logger.info("=== PRICING CALCULATION RESULTS ===");
        logger.info("Total calculations: {}", results.size());
        
        // Group results by currency and type for better readability
        Map<String, List<PriceCalculationResult>> resultsByGroup = results.stream()
                .collect(Collectors.groupingBy(r -> r.getCurrency() + ":" + r.getType()));
        
        for (Map.Entry<String, List<PriceCalculationResult>> entry : resultsByGroup.entrySet()) {
            String groupKey = entry.getKey();
            List<PriceCalculationResult> groupResults = entry.getValue();
            
            logger.info("--- {} ---", groupKey);
            
            for (PriceCalculationResult result : groupResults) {
                logger.info("  {} [{}] Vol:{} | Bid: {} | Ask: {} | Strategy: {}", 
                        result.getSource(),
                        result.getCurrency() + " " + result.getType(),
                        result.getVolume(),
                        result.getCalculatedBidRate(),
                        result.getCalculatedAskRate(),
                        result.getStrategy());
            }
        }
        
        logger.info("=== END PRICING RESULTS ===");
    }
    
    /**
     * Manual trigger for pricing calculation (for testing)
     */
    public List<PriceCalculationResult> calculatePricingManually() {
        logger.info("Manual pricing calculation triggered");
        
        List<TickData> allTickData = redisTickDataService.getAllTickData();
        Map<String, List<TickData>> groupedTickData = redisTickDataService.groupTickData(allTickData);
        List<PriceCalculationResult> results = new ArrayList<>();
        
        for (Map.Entry<String, List<TickData>> entry : groupedTickData.entrySet()) {
            String groupKey = entry.getKey();
            List<TickData> ticks = entry.getValue();
            
            Map<String, List<TickData>> ticksBySource = ticks.stream()
                    .collect(Collectors.groupingBy(TickData::getSource));
            
            for (String source : Arrays.asList("P1", "P2", "P3")) {
                List<TickData> sourceTicks = ticksBySource.get(source);
                
                if (sourceTicks != null && !sourceTicks.isEmpty()) {
                    PriceCalculationResult result = processSourceTicks(source, sourceTicks, groupKey);
                    if (result != null) {
                        results.add(result);
                    }
                }
            }
        }
        
        return results;
    }
}