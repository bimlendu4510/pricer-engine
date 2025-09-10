package com.pricer.service;

import com.pricer.config.PricingConfig;
import com.pricer.model.PriceCalculationResult;
import com.pricer.model.PricingStrategy;
import com.pricer.model.TickData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that implements all pricing calculation strategies.
 */
@Service
public class PricingCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(PricingCalculationService.class);

    @Autowired
    private PricingConfig pricingConfig;

    /**
     * Processes tick data and calculates pricing results for all configured sources.
     */
    public List<PriceCalculationResult> calculatePricing(List<TickData> tickDataList) {
        if (tickDataList == null || tickDataList.isEmpty()) {
            logger.info("No tick data provided for pricing calculation");
            return new ArrayList<>();
        }

        List<PriceCalculationResult> results = new ArrayList<>();
        
        // Group tick data by currency, type, and volume
        Map<String, List<TickData>> groupedData = groupTickData(tickDataList);
        
        // Process each group
        for (Map.Entry<String, List<TickData>> entry : groupedData.entrySet()) {
            String groupKey = entry.getKey();
            List<TickData> ticks = entry.getValue();
            
            if (ticks.isEmpty()) continue;
            
            // Parse group key: currency-type-volume
            String[] keyParts = groupKey.split("-");
            if (keyParts.length != 3) continue;
            
            String currency = keyParts[0];
            String type = keyParts[1];
            Double volume = Double.parseDouble(keyParts[2]);
            
            // Calculate pricing for each source that has data in this group
            Map<String, List<TickData>> sourceData = ticks.stream()
                    .collect(Collectors.groupingBy(TickData::getSource));
                    
            for (Map.Entry<String, List<TickData>> sourceEntry : sourceData.entrySet()) {
                String source = sourceEntry.getKey();
                List<TickData> sourceTicks = sourceEntry.getValue();
                
                PricingStrategy strategy = pricingConfig.getStrategyForSource(source);
                if (strategy != null) {
                    PriceCalculationResult result = calculatePricingForSource(
                            source, currency, type, volume, sourceTicks, strategy);
                    if (result != null) {
                        results.add(result);
                    }
                }
            }
        }
        
        logger.info("Calculated pricing for {} groups with {} total results", groupedData.size(), results.size());
        return results;
    }

    /**
     * Groups tick data by currency, type, and volume.
     */
    private Map<String, List<TickData>> groupTickData(List<TickData> tickDataList) {
        return tickDataList.stream()
                .collect(Collectors.groupingBy(tick -> 
                        tick.getCurrency() + "-" + tick.getType() + "-" + tick.getVolume()));
    }

    /**
     * Calculates pricing for a specific source using the configured strategy.
     */
    private PriceCalculationResult calculatePricingForSource(String source, String currency, String type,
                                                             Double volume, List<TickData> ticks, PricingStrategy strategy) {
        if (ticks.isEmpty()) {
            return null;
        }

        Double bidRate = null;
        Double askRate = null;

        switch (strategy) {
            case BEST_PRICE:
                bidRate = calculateBestBid(ticks);
                askRate = calculateBestAsk(ticks);
                break;
            case WORST_PRICE:
                bidRate = calculateWorstBid(ticks);
                askRate = calculateWorstAsk(ticks);
                break;
            case VWAP:
                bidRate = calculateVWAPBid(ticks);
                askRate = calculateVWAPAsk(ticks);
                break;
            case DEEP_AVG:
                bidRate = calculateDeepAvgBid(ticks);
                askRate = calculateDeepAvgAsk(ticks);
                break;
            case DEEP_VWAP:
                bidRate = calculateDeepVWAPBid(ticks);
                askRate = calculateDeepVWAPAsk(ticks);
                break;
            default:
                logger.warn("Unknown pricing strategy: {}", strategy);
                return null;
        }

        if (bidRate != null && askRate != null) {
            return new PriceCalculationResult(source, currency, type, volume, strategy, 
                                              bidRate, askRate, ticks.size());
        }
        
        return null;
    }

    // Best Price Strategy Implementation
    private Double calculateBestBid(List<TickData> ticks) {
        // For bid: Highest bid rate (better for seller)
        return ticks.stream()
                .filter(tick -> tick.getBidRate() != null)
                .mapToDouble(TickData::getBidRate)
                .max()
                .orElse(0.0);
    }

    private Double calculateBestAsk(List<TickData> ticks) {
        // For ask: Lowest ask rate (better for buyer)
        return ticks.stream()
                .filter(tick -> tick.getAskRate() != null)
                .mapToDouble(TickData::getAskRate)
                .min()
                .orElse(0.0);
    }

    // Worst Price Strategy Implementation
    private Double calculateWorstBid(List<TickData> ticks) {
        // For bid: Lowest bid rate (worse for seller)
        return ticks.stream()
                .filter(tick -> tick.getBidRate() != null)
                .mapToDouble(TickData::getBidRate)
                .min()
                .orElse(0.0);
    }

    private Double calculateWorstAsk(List<TickData> ticks) {
        // For ask: Highest ask rate (worse for buyer)
        return ticks.stream()
                .filter(tick -> tick.getAskRate() != null)
                .mapToDouble(TickData::getAskRate)
                .max()
                .orElse(0.0);
    }

    // VWAP Strategy Implementation
    private Double calculateVWAPBid(List<TickData> ticks) {
        // VWAP = (Σ Tick Price × Tick Volume) / Σ Tick Volume
        double totalWeightedPrice = 0.0;
        double totalVolume = 0.0;
        
        for (TickData tick : ticks) {
            if (tick.getBidRate() != null && tick.getVolume() != null) {
                totalWeightedPrice += tick.getBidRate() * tick.getVolume();
                totalVolume += tick.getVolume();
            }
        }
        
        return totalVolume > 0 ? totalWeightedPrice / totalVolume : 0.0;
    }

    private Double calculateVWAPAsk(List<TickData> ticks) {
        double totalWeightedPrice = 0.0;
        double totalVolume = 0.0;
        
        for (TickData tick : ticks) {
            if (tick.getAskRate() != null && tick.getVolume() != null) {
                totalWeightedPrice += tick.getAskRate() * tick.getVolume();
                totalVolume += tick.getVolume();
            }
        }
        
        return totalVolume > 0 ? totalWeightedPrice / totalVolume : 0.0;
    }

    // Deep Avg Strategy Implementation (Last 3 Worst)
    private Double calculateDeepAvgBid(List<TickData> ticks) {
        // Average of the last three worst prices (lowest bids)
        List<Double> bidRates = ticks.stream()
                .filter(tick -> tick.getBidRate() != null)
                .map(TickData::getBidRate)
                .sorted() // Sort ascending for worst (lowest) bids
                .collect(Collectors.toList());
                
        return calculateAverageOfLastN(bidRates, 3);
    }

    private Double calculateDeepAvgAsk(List<TickData> ticks) {
        // Average of the last three worst prices (highest asks)
        List<Double> askRates = ticks.stream()
                .filter(tick -> tick.getAskRate() != null)
                .map(TickData::getAskRate)
                .sorted(Collections.reverseOrder()) // Sort descending for worst (highest) asks
                .collect(Collectors.toList());
                
        return calculateAverageOfLastN(askRates, 3);
    }

    // Deep VWAP Strategy Implementation (Last 3 Worst Ticks)
    private Double calculateDeepVWAPBid(List<TickData> ticks) {
        // Volume weighted average for last three worst price ticks (lowest bids)
        List<TickData> sortedTicks = ticks.stream()
                .filter(tick -> tick.getBidRate() != null && tick.getVolume() != null)
                .sorted(Comparator.comparing(TickData::getBidRate)) // Sort by bid rate ascending
                .collect(Collectors.toList());
                
        List<TickData> lastThree = getLastNTicks(sortedTicks, 3);
        return calculateVWAPBid(lastThree);
    }

    private Double calculateDeepVWAPAsk(List<TickData> ticks) {
        // Volume weighted average for last three worst price ticks (highest asks)
        List<TickData> sortedTicks = ticks.stream()
                .filter(tick -> tick.getAskRate() != null && tick.getVolume() != null)
                .sorted(Comparator.comparing(TickData::getAskRate).reversed()) // Sort by ask rate descending
                .collect(Collectors.toList());
                
        List<TickData> lastThree = getLastNTicks(sortedTicks, 3);
        return calculateVWAPAsk(lastThree);
    }

    // Helper methods
    private Double calculateAverageOfLastN(List<Double> rates, int n) {
        if (rates.isEmpty()) {
            return 0.0;
        }
        
        int start = Math.max(0, rates.size() - n);
        List<Double> lastN = rates.subList(start, rates.size());
        
        return lastN.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private List<TickData> getLastNTicks(List<TickData> ticks, int n) {
        if (ticks.size() <= n) {
            return ticks;
        }
        return ticks.subList(ticks.size() - n, ticks.size());
    }
}