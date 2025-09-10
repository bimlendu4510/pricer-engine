package com.pricer.demo;

import com.pricer.config.PricingConfig;
import com.pricer.model.PriceCalculationResult;
import com.pricer.model.PricingStrategy;
import com.pricer.model.TickData;
import com.pricer.service.PricingCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Standalone demo that doesn't require Spring Boot or Redis
 */
public class StandalonePricingDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(StandalonePricingDemo.class);
    
    public static void main(String[] args) {
        logger.info("=== STANDALONE PRICING ENGINE DEMO ===");
        
        StandalonePricingDemo demo = new StandalonePricingDemo();
        demo.runDemo();
    }
    
    public void runDemo() {
        // Create services
        PricingCalculationService pricingService = new PricingCalculationService();
        PricingConfig config = new PricingConfig();
        
        // Log configuration
        logConfiguration(config);
        
        // Create sample tick data as per problem statement
        List<TickData> sampleData = createSampleTickData();
        
        // Group data by currency, type, and volume
        Map<String, List<TickData>> groupedData = groupTickData(sampleData);
        
        // Process each group with different strategies
        List<PriceCalculationResult> results = new ArrayList<>();
        
        for (Map.Entry<String, List<TickData>> entry : groupedData.entrySet()) {
            String groupKey = entry.getKey();
            List<TickData> ticks = entry.getValue();
            
            logger.info("Processing group: {} with {} ticks", groupKey, ticks.size());
            
            // Extract currency, type, volume from group key
            String[] parts = groupKey.split(":");
            String currency = parts[0];
            String type = parts[1]; 
            BigDecimal volume = new BigDecimal(parts[2]);
            
            // Group by source and apply strategy for each source
            Map<String, List<TickData>> ticksBySource = new HashMap<>();
            for (TickData tick : ticks) {
                ticksBySource.computeIfAbsent(tick.getSource(), k -> new ArrayList<>()).add(tick);
            }
            
            // Process P1, P2, P3 sources
            for (String source : Arrays.asList("P1", "P2", "P3")) {
                List<TickData> sourceTicks = ticksBySource.get(source);
                
                if (sourceTicks != null && !sourceTicks.isEmpty()) {
                    PricingStrategy strategy = config.getStrategyForSource(source);
                    
                    PriceCalculationResult result = pricingService.calculatePrice(
                            sourceTicks, strategy, currency, type, volume);
                    
                    if (result != null) {
                        result.setSource(source);
                        results.add(result);
                    }
                }
            }
        }
        
        // Log results
        logDetailedResults(results);
        
        logger.info("=== DEMO COMPLETED ===");
    }
    
    /**
     * Create sample tick data matching the Redis keys from problem statement
     */
    private List<TickData> createSampleTickData() {
        List<TickData> tickData = new ArrayList<>();
        
        // Sample data matching the Redis keys from problem statement
        Object[][] sampleKeys = {
            {"P2", "USD.INR", "SPOT", "20000.0"},
            {"P1", "USD.INR", "FORWARD", "10000.0"},
            {"P3", "USD.INR", "FORWARD", "30000.0"},
            {"P1", "USD.INR", "SPOT", "1000000.0"},
            {"P3", "USD.INR", "SPOT", "30000.0"},
            {"P1", "USD.INR", "FORWARD", "30000.0"},
            {"P2", "USD.INR", "FORWARD", "30000.0"},
            {"P2", "USD.INR", "SPOT", "10000.0"},
            {"P1", "USD.INR", "FORWARD", "20000.0"},
            {"P3", "USD.INR", "SPOT", "10000.0"},
            {"P2", "USD.INR", "FORWARD", "10000.0"},
            {"P3", "USD.INR", "FORWARD", "1000000.0"},
            {"P1", "USD.INR", "SPOT", "10000.0"},
            {"P2", "USD.INR", "SPOT", "1000000.0"},
            {"P2", "USD.INR", "FORWARD", "20000.0"},
            {"P1", "USD.INR", "FORWARD", "1000000.0"},
            {"P2", "USD.INR", "SPOT", "30000.0"},
            {"P3", "USD.INR", "FORWARD", "20000.0"},
            {"P3", "USD.INR", "SPOT", "1000000.0"},
            {"P3", "USD.INR", "FORWARD", "10000.0"},
            {"P1", "USD.INR", "SPOT", "20000.0"},
            {"P1", "USD.INR", "SPOT", "30000.0"},
            {"P2", "USD.INR", "FORWARD", "1000000.0"},
            {"P3", "USD.INR", "SPOT", "20000.0"}
        };
        
        for (Object[] keyData : sampleKeys) {
            String source = (String) keyData[0];
            String currency = (String) keyData[1];
            String type = (String) keyData[2];
            BigDecimal volume = new BigDecimal((String) keyData[3]);
            
            // Generate mock rates
            BigDecimal baseRate = new BigDecimal("82.50");
            
            BigDecimal sourceSpread;
            switch (source) {
                case "P1":
                    sourceSpread = new BigDecimal("0.02");
                    break;
                case "P2": 
                    sourceSpread = new BigDecimal("0.05");
                    break;
                case "P3":
                    sourceSpread = new BigDecimal("0.10");
                    break;
                default:
                    sourceSpread = new BigDecimal("0.05");
                    break;
            }
            
            BigDecimal volumeAdjustment = volume.compareTo(new BigDecimal("100000")) > 0 ? 
                    new BigDecimal("0.01") : new BigDecimal("0.03");
            
            BigDecimal typeAdjustment = "FORWARD".equals(type) ? 
                    new BigDecimal("0.02") : BigDecimal.ZERO;
            
            // Add some randomness for variety
            Random random = new Random(source.hashCode() + type.hashCode() + volume.hashCode());
            BigDecimal randomAdjustment = new BigDecimal(random.nextDouble() * 0.02 - 0.01); // -0.01 to +0.01
            
            BigDecimal bidRate = baseRate.subtract(sourceSpread).add(volumeAdjustment)
                    .add(typeAdjustment).add(randomAdjustment);
            BigDecimal askRate = baseRate.add(sourceSpread).add(volumeAdjustment)
                    .add(typeAdjustment).add(randomAdjustment);
            
            tickData.add(new TickData(source, currency, type, volume, bidRate, askRate, LocalDateTime.now()));
        }
        
        logger.info("Created {} sample tick data records", tickData.size());
        return tickData;
    }
    
    /**
     * Group tick data by currency:type:volume
     */
    private Map<String, List<TickData>> groupTickData(List<TickData> tickData) {
        Map<String, List<TickData>> grouped = new HashMap<>();
        
        for (TickData tick : tickData) {
            String key = tick.getCurrency() + ":" + tick.getType() + ":" + tick.getVolume();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(tick);
        }
        
        return grouped;
    }
    
    /**
     * Log configuration
     */
    private void logConfiguration(PricingConfig config) {
        logger.info("=== PRICING CONFIGURATION ===");
        config.getSourceStrategies().forEach((source, strategy) -> {
            logger.info("  {} -> {}: {}", source, strategy, strategy.getDescription());
        });
    }
    
    /**
     * Log detailed results
     */
    private void logDetailedResults(List<PriceCalculationResult> results) {
        logger.info("=== PRICING CALCULATION RESULTS ===");
        logger.info("Total calculations: {}", results.size());
        
        // Group results by currency and type
        Map<String, Map<String, Map<String, PriceCalculationResult>>> groupedResults = new HashMap<>();
        
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
                    logger.info("    {} [{}]: Vol={} | Bid={} | Ask={} | {}",
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
        long p1Count = results.stream().filter(r -> "P1".equals(r.getSource())).count();
        long p2Count = results.stream().filter(r -> "P2".equals(r.getSource())).count();
        long p3Count = results.stream().filter(r -> "P3".equals(r.getSource())).count();
        
        logger.info("Results by source: P1={} (BEST_PRICE), P2={} (DEEP_AVG), P3={} (VWAP)", p1Count, p2Count, p3Count);
        
        // Show unique volume/type combinations processed
        Set<String> combinations = new HashSet<>();
        for (PriceCalculationResult result : results) {
            combinations.add(result.getType() + ":" + result.getVolume());
        }
        logger.info("Unique currency:type:volume combinations processed: {}", combinations.size());
        combinations.forEach(combo -> logger.info("  - USD.INR:{}", combo));
    }
}