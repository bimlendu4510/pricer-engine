package com.pricer.demo;

import com.pricer.config.PricingConfig;
import com.pricer.model.PriceCalculationResult;
import com.pricer.model.PricingStrategy;
import com.pricer.model.TickData;
import com.pricer.service.PricingCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Standalone demo that can run without Redis.
 * Generates sample tick data based on the problem statement.
 */
public class StandalonePricingDemo {

    private static final Logger logger = LoggerFactory.getLogger(StandalonePricingDemo.class);

    public static void main(String[] args) {
        logger.info("Starting Standalone Pricing Demo...");
        
        StandalonePricingDemo demo = new StandalonePricingDemo();
        demo.runDemo();
    }

    public void runDemo() {
        // Initialize configuration and service
        PricingConfig pricingConfig = new PricingConfig();
        PricingCalculationService pricingService = new PricingCalculationService();
        
        // Manually inject configuration since we're not using Spring context
        try {
            java.lang.reflect.Field configField = PricingCalculationService.class.getDeclaredField("pricingConfig");
            configField.setAccessible(true);
            configField.set(pricingService, pricingConfig);
        } catch (Exception e) {
            logger.error("Error setting up configuration", e);
            return;
        }

        // Generate sample tick data
        List<TickData> sampleTickData = generateSampleTickData();
        
        // Display configuration
        displayConfiguration(pricingConfig);
        
        // Calculate pricing
        List<PriceCalculationResult> results = pricingService.calculatePricing(sampleTickData);
        
        // Display results
        displayResults(results);
    }

    /**
     * Generates sample tick data based on the problem statement Redis keys.
     */
    private List<TickData> generateSampleTickData() {
        List<TickData> tickDataList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Sample data for USD.INR SPOT with various volumes
        // P1, P2, P3 sources with different volumes as mentioned in problem statement
        
        // Large volume data (1M)
        tickDataList.addAll(generateTicksForGroup("USD.INR", "SPOT", 1000000.0, now));
        
        // Medium volume data
        tickDataList.addAll(generateTicksForGroup("USD.INR", "SPOT", 500000.0, now.minusMinutes(5)));
        
        // Small volume data from problem statement
        tickDataList.addAll(generateTicksForGroup("USD.INR", "FORWARD", 10000.0, now.minusMinutes(10)));
        tickDataList.addAll(generateTicksForGroup("USD.INR", "FORWARD", 20000.0, now.minusMinutes(15)));
        tickDataList.addAll(generateTicksForGroup("USD.INR", "FORWARD", 30000.0, now.minusMinutes(20)));
        
        logger.info("Generated {} sample tick data entries", tickDataList.size());
        return tickDataList;
    }

    /**
     * Generates tick data for a specific currency, type, and volume group.
     */
    private List<TickData> generateTicksForGroup(String currency, String type, Double volume, LocalDateTime timestamp) {
        List<TickData> ticks = new ArrayList<>();
        Random random = new Random();
        
        // Base rates for USD.INR
        double baseBid = 82.40;
        double baseAsk = 82.60;
        
        // Generate multiple ticks for each source (P1, P2, P3)
        String[] sources = {"P1", "P2", "P3"};
        
        for (String source : sources) {
            // Generate 3-5 ticks per source to provide variety for calculations
            int tickCount = 3 + random.nextInt(3); // 3-5 ticks
            
            for (int i = 0; i < tickCount; i++) {
                // Add some variance to rates
                double bidVariance = (random.nextDouble() - 0.5) * 0.30; // ±0.15 variance
                double askVariance = (random.nextDouble() - 0.5) * 0.30;
                
                double bidRate = baseBid + bidVariance;
                double askRate = baseAsk + askVariance;
                
                // Ensure ask > bid
                if (askRate <= bidRate) {
                    askRate = bidRate + 0.05 + random.nextDouble() * 0.10;
                }
                
                TickData tick = new TickData(
                    source, currency, type, volume,
                    Math.round(bidRate * 100.0) / 100.0, // Round to 2 decimal places
                    Math.round(askRate * 100.0) / 100.0,
                    timestamp.minusSeconds(i * 30) // Spread ticks over time
                );
                
                ticks.add(tick);
            }
        }
        
        return ticks;
    }

    /**
     * Displays pricing configuration.
     */
    private void displayConfiguration(PricingConfig config) {
        System.out.println();
        System.out.println("=== PRICING CONFIGURATION ===");
        
        Map<String, PricingStrategy> strategies = config.getSourceStrategies();
        for (Map.Entry<String, PricingStrategy> entry : strategies.entrySet()) {
            System.out.printf("%s -> %s%n", entry.getKey(), entry.getValue().toString());
        }
        System.out.println();
    }

    /**
     * Displays pricing calculation results.
     */
    private void displayResults(List<PriceCalculationResult> results) {
        if (results.isEmpty()) {
            System.out.println("No pricing results calculated.");
            return;
        }

        System.out.println("=== PRICING CALCULATION RESULTS ===");
        
        // Group results by currency and type for better readability
        Map<String, Map<String, List<PriceCalculationResult>>> groupedResults = results.stream()
                .collect(Collectors.groupingBy(
                        PriceCalculationResult::getCurrency,
                        Collectors.groupingBy(PriceCalculationResult::getType)
                ));
        
        for (Map.Entry<String, Map<String, List<PriceCalculationResult>>> currencyEntry : groupedResults.entrySet()) {
            String currency = currencyEntry.getKey();
            System.out.printf("Currency: %s%n", currency);
            
            for (Map.Entry<String, List<PriceCalculationResult>> typeEntry : currencyEntry.getValue().entrySet()) {
                String type = typeEntry.getKey();
                System.out.printf("  Type: %s%n", type);
                
                // Sort results by source for consistent output
                List<PriceCalculationResult> sortedResults = typeEntry.getValue().stream()
                        .sorted(Comparator.comparing(PriceCalculationResult::getSource))
                        .collect(Collectors.toList());
                
                for (PriceCalculationResult result : sortedResults) {
                    System.out.printf("    %s [%s]: Vol=%.1f | Bid=%.2f | Ask=%.2f%n",
                            result.getSource(),
                            result.getStrategy().name(),
                            result.getVolume(),
                            result.getBidRate(),
                            result.getAskRate());
                }
            }
        }
        
        // Display summary by source
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
        
        System.out.println(summaryString);
        System.out.println();
    }
}