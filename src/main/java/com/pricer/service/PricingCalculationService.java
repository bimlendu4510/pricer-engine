package com.pricer.service;

import com.pricer.model.PriceCalculationResult;
import com.pricer.model.PricingStrategy;
import com.pricer.model.TickData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PricingCalculationService {
    
    private static final Logger logger = LoggerFactory.getLogger(PricingCalculationService.class);
    private static final int SCALE = 6; // Decimal precision for calculations

    /**
     * Calculate price based on the specified strategy
     */
    public PriceCalculationResult calculatePrice(List<TickData> ticks, PricingStrategy strategy,
                                                 String currency, String type, BigDecimal volume) {
        
        if (ticks == null || ticks.isEmpty()) {
            logger.warn("No tick data available for calculation");
            return null;
        }

        PriceCalculationResult result = new PriceCalculationResult();
        result.setCurrency(currency);
        result.setType(type);
        result.setVolume(volume);
        result.setStrategy(strategy);

        switch (strategy) {
            case BEST_PRICE:
                calculateBestPrice(ticks, result);
                break;
            case WORST_PRICE:
                calculateWorstPrice(ticks, result);
                break;
            case VWAP:
                calculateVWAP(ticks, result);
                break;
            case DEEP_AVG:
                calculateDeepAvg(ticks, result);
                break;
            case DEEP_VWAP:
                calculateDeepVWAP(ticks, result);
                break;
            default:
                logger.error("Unknown pricing strategy: {}", strategy);
                return null;
        }

        return result;
    }

    /**
     * Best Price: Most favourable rate per currency per volume at any moment
     * For bid: highest bid rate (better for seller)
     * For ask: lowest ask rate (better for buyer)
     */
    private void calculateBestPrice(List<TickData> ticks, PriceCalculationResult result) {
        // Best bid = highest bid rate
        BigDecimal bestBid = ticks.stream()
                .map(TickData::getBidRate)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Best ask = lowest ask rate  
        BigDecimal bestAsk = ticks.stream()
                .map(TickData::getAskRate)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        result.setCalculatedBidRate(bestBid);
        result.setCalculatedAskRate(bestAsk);
        result.setDescription("Best Price - Highest Bid: " + bestBid + ", Lowest Ask: " + bestAsk);
        
        logger.debug("Best Price calculated - Bid: {}, Ask: {}", bestBid, bestAsk);
    }

    /**
     * Worst Price: Least favourable rate per currency per volume at any moment
     * For bid: lowest bid rate (worse for seller)
     * For ask: highest ask rate (worse for buyer)
     */
    private void calculateWorstPrice(List<TickData> ticks, PriceCalculationResult result) {
        // Worst bid = lowest bid rate
        BigDecimal worstBid = ticks.stream()
                .map(TickData::getBidRate)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Worst ask = highest ask rate
        BigDecimal worstAsk = ticks.stream()
                .map(TickData::getAskRate)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        result.setCalculatedBidRate(worstBid);
        result.setCalculatedAskRate(worstAsk);
        result.setDescription("Worst Price - Lowest Bid: " + worstBid + ", Highest Ask: " + worstAsk);
        
        logger.debug("Worst Price calculated - Bid: {}, Ask: {}", worstBid, worstAsk);
    }

    /**
     * VWAP: Volume Weighted Average Price
     * Formula: VWAP = (Σ Tick Price × Tick Volume) / Σ Tick Volume
     */
    private void calculateVWAP(List<TickData> ticks, PriceCalculationResult result) {
        BigDecimal totalBidValue = BigDecimal.ZERO;
        BigDecimal totalAskValue = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;

        for (TickData tick : ticks) {
            BigDecimal volume = tick.getVolume();
            totalBidValue = totalBidValue.add(tick.getBidRate().multiply(volume));
            totalAskValue = totalAskValue.add(tick.getAskRate().multiply(volume));
            totalVolume = totalVolume.add(volume);
        }

        if (totalVolume.compareTo(BigDecimal.ZERO) == 0) {
            result.setCalculatedBidRate(BigDecimal.ZERO);
            result.setCalculatedAskRate(BigDecimal.ZERO);
            result.setDescription("VWAP - No volume data available");
            return;
        }

        BigDecimal vwapBid = totalBidValue.divide(totalVolume, SCALE, RoundingMode.HALF_UP);
        BigDecimal vwapAsk = totalAskValue.divide(totalVolume, SCALE, RoundingMode.HALF_UP);

        result.setCalculatedBidRate(vwapBid);
        result.setCalculatedAskRate(vwapAsk);
        result.setDescription("VWAP - Bid: " + vwapBid + ", Ask: " + vwapAsk + ", Total Volume: " + totalVolume);
        
        logger.debug("VWAP calculated - Bid: {}, Ask: {}, Total Volume: {}", vwapBid, vwapAsk, totalVolume);
    }

    /**
     * Deep Avg: Average of last 3 worst prices
     * Formula: Deep Avg = (Σ of last 3 worst prices) / 3
     */
    private void calculateDeepAvg(List<TickData> ticks, PriceCalculationResult result) {
        // Sort by worst prices first
        List<TickData> sortedByWorstBid = ticks.stream()
                .sorted(Comparator.comparing(TickData::getBidRate))
                .collect(Collectors.toList());

        List<TickData> sortedByWorstAsk = ticks.stream()
                .sorted(Comparator.comparing(TickData::getAskRate).reversed())
                .collect(Collectors.toList());

        // Get last 3 worst for bid (lowest 3)
        List<BigDecimal> last3WorstBids = sortedByWorstBid.stream()
                .limit(3)
                .map(TickData::getBidRate)
                .collect(Collectors.toList());

        // Get last 3 worst for ask (highest 3)  
        List<BigDecimal> last3WorstAsks = sortedByWorstAsk.stream()
                .limit(3)
                .map(TickData::getAskRate)
                .collect(Collectors.toList());

        BigDecimal avgWorstBid = last3WorstBids.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(last3WorstBids.size()), SCALE, RoundingMode.HALF_UP);

        BigDecimal avgWorstAsk = last3WorstAsks.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(last3WorstAsks.size()), SCALE, RoundingMode.HALF_UP);

        result.setCalculatedBidRate(avgWorstBid);
        result.setCalculatedAskRate(avgWorstAsk);
        result.setDescription("Deep Avg - Last 3 Worst Avg Bid: " + avgWorstBid + ", Ask: " + avgWorstAsk);
        
        logger.debug("Deep Avg calculated - Bid: {}, Ask: {}", avgWorstBid, avgWorstAsk);
    }

    /**
     * Deep VWAP: Volume weighted average price for last 3 worst price ticks
     * Formula: Deep VWAP = (Σ Last 3 (Tick Price × Tick Volume)) / Σ Last 3 Volumes
     */
    private void calculateDeepVWAP(List<TickData> ticks, PriceCalculationResult result) {
        // Sort by worst prices first
        List<TickData> sortedByWorstBid = ticks.stream()
                .sorted(Comparator.comparing(TickData::getBidRate))
                .limit(3)
                .collect(Collectors.toList());

        List<TickData> sortedByWorstAsk = ticks.stream()
                .sorted(Comparator.comparing(TickData::getAskRate).reversed())
                .limit(3)
                .collect(Collectors.toList());

        // Calculate VWAP for worst 3 bids
        BigDecimal totalBidValue = BigDecimal.ZERO;
        BigDecimal totalBidVolume = BigDecimal.ZERO;
        for (TickData tick : sortedByWorstBid) {
            totalBidValue = totalBidValue.add(tick.getBidRate().multiply(tick.getVolume()));
            totalBidVolume = totalBidVolume.add(tick.getVolume());
        }

        // Calculate VWAP for worst 3 asks
        BigDecimal totalAskValue = BigDecimal.ZERO;
        BigDecimal totalAskVolume = BigDecimal.ZERO;
        for (TickData tick : sortedByWorstAsk) {
            totalAskValue = totalAskValue.add(tick.getAskRate().multiply(tick.getVolume()));
            totalAskVolume = totalAskVolume.add(tick.getVolume());
        }

        BigDecimal deepVwapBid = totalBidVolume.compareTo(BigDecimal.ZERO) == 0 ? 
                BigDecimal.ZERO : totalBidValue.divide(totalBidVolume, SCALE, RoundingMode.HALF_UP);
        
        BigDecimal deepVwapAsk = totalAskVolume.compareTo(BigDecimal.ZERO) == 0 ? 
                BigDecimal.ZERO : totalAskValue.divide(totalAskVolume, SCALE, RoundingMode.HALF_UP);

        result.setCalculatedBidRate(deepVwapBid);
        result.setCalculatedAskRate(deepVwapAsk);
        result.setDescription("Deep VWAP - Last 3 Worst VWAP Bid: " + deepVwapBid + ", Ask: " + deepVwapAsk);
        
        logger.debug("Deep VWAP calculated - Bid: {}, Ask: {}", deepVwapBid, deepVwapAsk);
    }
}