package com.pricer.scheduler;

import com.pricer.model.PriceCalculationResult;
import com.pricer.model.TickData;
import com.pricer.service.PricingCalculationService;
import com.pricer.service.RedisTickDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scheduler for automated pricing calculations.
 * Runs every 30 seconds when Redis is configured.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "spring.redis.host")
public class PricingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PricingScheduler.class);

    @Autowired
    private RedisTickDataService redisTickDataService;

    @Autowired
    private PricingCalculationService pricingCalculationService;

    /**
     * Scheduled method that runs every 30 seconds to process pricing calculations.
     */
    @Scheduled(fixedRate = 30000) // 30 seconds
    public void calculatePricing() {
        logger.info("Starting scheduled pricing calculation...");
        
        try {
            // Retrieve tick data from Redis
            List<TickData> tickDataList = redisTickDataService.getAllTickData();
            
            if (tickDataList.isEmpty()) {
                logger.info("No tick data available for pricing calculation");
                return;
            }
            
            // Calculate pricing for all sources
            List<PriceCalculationResult> results = pricingCalculationService.calculatePricing(tickDataList);
            
            if (!results.isEmpty()) {
                logPricingResults(results);
            } else {
                logger.warn("No pricing results calculated");
            }
            
        } catch (Exception e) {
            logger.error("Error during scheduled pricing calculation", e);
        }
    }

    /**
     * Logs pricing results in a structured format.
     */
    private void logPricingResults(List<PriceCalculationResult> results) {
        logger.info("=== PRICING CALCULATION RESULTS ===");
        
        // Group results by currency and type for better readability
        Map<String, Map<String, List<PriceCalculationResult>>> groupedResults = results.stream()
                .collect(Collectors.groupingBy(
                        PriceCalculationResult::getCurrency,
                        Collectors.groupingBy(PriceCalculationResult::getType)
                ));
        
        for (Map.Entry<String, Map<String, List<PriceCalculationResult>>> currencyEntry : groupedResults.entrySet()) {
            String currency = currencyEntry.getKey();
            logger.info("Currency: {}", currency);
            
            for (Map.Entry<String, List<PriceCalculationResult>> typeEntry : currencyEntry.getValue().entrySet()) {
                String type = typeEntry.getKey();
                logger.info("  Type: {}", type);
                
                for (PriceCalculationResult result : typeEntry.getValue()) {
                    logger.info("    {} [{}]: Vol={} | Bid={:.2f} | Ask={:.2f}",
                            result.getSource(),
                            result.getStrategy().name(),
                            result.getVolume(),
                            result.getBidRate(),
                            result.getAskRate());
                }
            }
        }
        
        // Log summary by source
        Map<String, Long> sourceCount = results.stream()
                .collect(Collectors.groupingBy(PriceCalculationResult::getSource, Collectors.counting()));
                
        Map<String, String> sourceStrategies = results.stream()
                .collect(Collectors.toMap(
                        PriceCalculationResult::getSource,
                        result -> result.getStrategy().name(),
                        (existing, replacement) -> existing
                ));
        
        StringBuilder summary = new StringBuilder("Results by source: ");
        sourceCount.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String source = entry.getKey();
                    Long count = entry.getValue();
                    String strategy = sourceStrategies.get(source);
                    summary.append(String.format("%s=%d (%s), ", source, count, strategy));
                });
        
        // Remove trailing comma and space
        String summaryString = summary.toString();
        if (summaryString.endsWith(", ")) {
            summaryString = summaryString.substring(0, summaryString.length() - 2);
        }
        
        logger.info(summaryString);
    }
}