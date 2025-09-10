package demo;

import model.StandardMarketRate;
import model.UsdInrRateProcessor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Demonstration class showing how to use the UsdInrRateProcessor
 * with various real-world scenarios
 */
public class UsdInrProcessorDemo {
    
    public static void main(String[] args) {
        UsdInrRateProcessor processor = new UsdInrRateProcessor();
        
        System.out.println("=== USD-INR Rate Processor Demonstration ===\n");
        
        // Demo 1: Normal market conditions with valid spread
        System.out.println("Demo 1: Normal Market - Half paisa spread (Condition 0)");
        StandardMarketRate rate1 = createRate("83.1250", "83.1300", null, null, null, null);
        demonstrateProcessing(processor, rate1);
        
        // Demo 2: Wide spread market
        System.out.println("Demo 2: Wide Spread Market - 3 paisa spread (Condition 1)");
        StandardMarketRate rate2 = createRate("83.1000", "83.1300", null, null, null, null);
        demonstrateProcessing(processor, rate2);
        
        // Demo 3: Price moved beyond last traded - Case 2a
        System.out.println("Demo 3: Price Movement - Bid < Ask <= Last Price (Condition 2a)");
        StandardMarketRate rate3 = createRate("83.1000", "83.1100", "83.1200", null, null, null);
        demonstrateProcessing(processor, rate3);
        
        // Demo 4: Price moved beyond last traded - Case 2b  
        System.out.println("Demo 4: Price Movement - Last Price <= Bid < Ask (Condition 2b)");
        StandardMarketRate rate4 = createRate("83.1100", "83.1200", "83.1000", null, null, null);
        demonstrateProcessing(processor, rate4);
        
        // Demo 5: Upward trending market
        System.out.println("Demo 5: Upward Trending Market (Condition 3)");
        StandardMarketRate rate5 = createRate("83.1000", "83.1080", null, "83.1050", "83.1100", "83.1150");
        demonstrateProcessing(processor, rate5);
        
        // Demo 6: Downward trending market
        System.out.println("Demo 6: Downward Trending Market (Condition 3)");
        StandardMarketRate rate6 = createRate("83.1000", "83.1080", null, "83.1150", "83.1100", "83.1050");
        demonstrateProcessing(processor, rate6);
        
        // Demo 7: Weighted average fallback
        System.out.println("Demo 7: Non-trending Market - Weighted Average (Condition 4)");
        StandardMarketRate rate7 = createRate("83.1000", "83.1080", null, "83.1050", "83.1200", "83.1080");
        demonstrateProcessing(processor, rate7);
        
        System.out.println("=== Demo completed ===");
    }
    
    private static void demonstrateProcessing(UsdInrRateProcessor processor, StandardMarketRate rate) {
        System.out.println("Input:  " + rate);
        
        StandardMarketRate processed = processor.processUsdInrRate(rate);
        
        System.out.println("Output: " + processed);
        
        // Show the change
        BigDecimal bidChange = processed.getBid().subtract(rate.getBid());
        BigDecimal askChange = processed.getAsk().subtract(rate.getAsk());
        System.out.printf("Changes: Bid %+.4f, Ask %+.4f%n", bidChange, askChange);
        System.out.println();
    }
    
    private static StandardMarketRate createRate(String bid, String ask, String lastPrice, 
                                               String primac1, String primac2, String primac3) {
        StandardMarketRate rate = new StandardMarketRate();
        rate.setSymbol("USD-INR");
        rate.setBid(new BigDecimal(bid));
        rate.setAsk(new BigDecimal(ask));
        rate.setTimestamp(LocalDateTime.now());
        
        if (lastPrice != null) {
            rate.setLastTradedPrice(new BigDecimal(lastPrice));
        }
        if (primac1 != null) {
            rate.setPrimac1(new BigDecimal(primac1));
        }
        if (primac2 != null) {
            rate.setPrimac2(new BigDecimal(primac2));
        }
        if (primac3 != null) {
            rate.setPrimac3(new BigDecimal(primac3));
        }
        
        return rate;
    }
}