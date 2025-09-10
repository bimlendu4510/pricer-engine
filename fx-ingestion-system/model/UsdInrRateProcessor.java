package model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * USD-INR Rate Processor implementing the specific business logic for USD-INR rate calculations.
 * Handles 4 main conditions as per business requirements.
 */
public class UsdInrRateProcessor {
    
    // Weights for condition 4 - weighted average calculation
    private static final BigDecimal WEIGHT_PRIMAC1 = new BigDecimal("0.8");
    private static final BigDecimal WEIGHT_PRIMAC2 = new BigDecimal("0.2");
    private static final BigDecimal WEIGHT_PRIMAC3 = new BigDecimal("0.05");
    
    // Spread thresholds in paisa
    private static final BigDecimal HALF_PAISA = new BigDecimal("0.5");
    private static final BigDecimal ONE_PAISA = new BigDecimal("1.0");
    private static final BigDecimal TWO_PAISA = new BigDecimal("2.0");
    
    // Spread values in rupee (to be applied to bid/ask)
    private static final BigDecimal SPREAD_HALF_PAISA_RUPEE = new BigDecimal("0.005"); // 0.5 paisa = 0.005 rupee
    private static final BigDecimal SPREAD_ONE_PAISA_RUPEE = new BigDecimal("0.01");   // 1 paisa = 0.01 rupee
    
    /**
     * Process USD-INR rate according to business logic conditions
     */
    public StandardMarketRate processUsdInrRate(StandardMarketRate rate) {
        if (rate == null || rate.getBid() == null || rate.getAsk() == null) {
            throw new IllegalArgumentException("Rate, bid and ask must not be null");
        }
        
        BigDecimal spreadInPaisa = rate.getSpreadInPaisa();
        
        // Create a copy to modify
        StandardMarketRate processedRate = copyRate(rate);
        
        // Condition 0: If Ask-Bid = 0.5 paise, keep as is
        if (spreadInPaisa.compareTo(HALF_PAISA) == 0) {
            return processedRate; // Return as is
        }
        
        // Condition 1: If Ask-Bid >= 2 paise, keep as is
        if (spreadInPaisa.compareTo(TWO_PAISA) >= 0) {
            return processedRate; // Return as is
        }
        
        // Condition 2: Bid Ask has moved beyond Last Update Price
        if (rate.getLastTradedPrice() != null) {
            StandardMarketRate condition2Result = applyCondition2(processedRate);
            if (condition2Result != null) {
                return condition2Result;
            }
        }
        
        // Condition 3: Trending market
        if (rate.getPrimac1() != null && rate.getPrimac2() != null && rate.getPrimac3() != null) {
            StandardMarketRate condition3Result = applyCondition3(processedRate);
            if (condition3Result != null) {
                return condition3Result;
            }
        }
        
        // Condition 4: Fallback - weighted average
        return applyCondition4(processedRate);
    }
    
    /**
     * Condition 2: Handle cases where bid/ask moves beyond last traded price
     */
    private StandardMarketRate applyCondition2(StandardMarketRate rate) {
        BigDecimal bid = rate.getBid();
        BigDecimal ask = rate.getAsk();
        BigDecimal lastPrice = rate.getLastTradedPrice();
        
        // Condition 2a: If Bid < Ask <= lastTradedPrice
        if (bid.compareTo(ask) < 0 && ask.compareTo(lastPrice) <= 0) {
            BigDecimal spreadToApply = calculateSpreadToApply(rate.getSpreadInPaisa());
            rate.setAsk(ask); // Keep ask as is
            rate.setBid(ask.subtract(spreadToApply)); // Bid = Ask - spread
            return rate;
        }
        
        // Condition 2b: If lastTradedPrice <= Bid < Ask
        if (lastPrice.compareTo(bid) <= 0 && bid.compareTo(ask) < 0) {
            BigDecimal spreadToApply = calculateSpreadToApply(rate.getSpreadInPaisa());
            rate.setBid(bid); // Keep bid as is
            rate.setAsk(bid.add(spreadToApply)); // Ask = Bid + spread
            return rate;
        }
        
        return null; // Condition 2 doesn't apply
    }
    
    /**
     * Condition 3: Handle trending market based on Primac values
     */
    private StandardMarketRate applyCondition3(StandardMarketRate rate) {
        BigDecimal p1 = rate.getPrimac1();
        BigDecimal p2 = rate.getPrimac2();
        BigDecimal p3 = rate.getPrimac3();
        
        // Check for upward trend: P3 > P2 > P1
        boolean upwardTrend = p3.compareTo(p2) > 0 && p2.compareTo(p1) > 0;
        
        // Check for downward trend: P3 < P2 < P1
        boolean downwardTrend = p3.compareTo(p2) < 0 && p2.compareTo(p1) < 0;
        
        if (upwardTrend || downwardTrend) {
            // Mid = Primac1
            BigDecimal mid = p1;
            BigDecimal spreadToApply = calculateSpreadToApply(rate.getSpreadInPaisa());
            
            // Apply spread around the mid point
            rate.setBid(mid.subtract(spreadToApply.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP)));
            rate.setAsk(mid.add(spreadToApply.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP)));
            
            return rate;
        }
        
        return null; // Condition 3 doesn't apply
    }
    
    /**
     * Condition 4: Fallback weighted average calculation
     */
    private StandardMarketRate applyCondition4(StandardMarketRate rate) {
        // If we have Primac values, use weighted average
        if (rate.getPrimac1() != null && rate.getPrimac2() != null && rate.getPrimac3() != null) {
            BigDecimal weightedMid = rate.getPrimac1().multiply(WEIGHT_PRIMAC1)
                    .add(rate.getPrimac2().multiply(WEIGHT_PRIMAC2))
                    .add(rate.getPrimac3().multiply(WEIGHT_PRIMAC3));
            
            BigDecimal spreadToApply = calculateSpreadToApply(rate.getSpreadInPaisa());
            
            // Apply spread around the weighted mid point
            rate.setBid(weightedMid.subtract(spreadToApply.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP)));
            rate.setAsk(weightedMid.add(spreadToApply.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP)));
        }
        // If no Primac values, keep current bid/ask but ensure proper spread
        else {
            BigDecimal currentMid = rate.getBid().add(rate.getAsk()).divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
            BigDecimal spreadToApply = calculateSpreadToApply(rate.getSpreadInPaisa());
            
            rate.setBid(currentMid.subtract(spreadToApply.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP)));
            rate.setAsk(currentMid.add(spreadToApply.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP)));
        }
        
        return rate;
    }
    
    /**
     * Calculate the spread to apply based on current spread rules
     */
    private BigDecimal calculateSpreadToApply(BigDecimal currentSpreadInPaisa) {
        // If Ask-Bid < 2 & > 1 paisa, apply 1 paisa spread (0.5 paisa either side)
        if (currentSpreadInPaisa.compareTo(ONE_PAISA) > 0 && currentSpreadInPaisa.compareTo(TWO_PAISA) < 0) {
            return SPREAD_ONE_PAISA_RUPEE;
        }
        
        // If Ask-Bid < 1 paisa & > 0 paisa, apply 0.5 paisa spread
        if (currentSpreadInPaisa.compareTo(BigDecimal.ZERO) > 0 && currentSpreadInPaisa.compareTo(ONE_PAISA) < 0) {
            return SPREAD_HALF_PAISA_RUPEE;
        }
        
        // Default: maintain current spread in rupee format
        return currentSpreadInPaisa.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }
    
    /**
     * Create a copy of the rate for processing
     */
    private StandardMarketRate copyRate(StandardMarketRate original) {
        StandardMarketRate copy = new StandardMarketRate();
        copy.setSymbol(original.getSymbol());
        copy.setBid(original.getBid());
        copy.setAsk(original.getAsk());
        copy.setLastTradedPrice(original.getLastTradedPrice());
        copy.setPrimac1(original.getPrimac1());
        copy.setPrimac2(original.getPrimac2());
        copy.setPrimac3(original.getPrimac3());
        copy.setTimestamp(original.getTimestamp());
        copy.setSource(original.getSource());
        return copy;
    }
}