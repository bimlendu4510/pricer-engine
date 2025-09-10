package com.pricer.service;

import com.pricer.model.PriceCalculationResult;
import com.pricer.model.PricingStrategy;
import com.pricer.model.TickData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PricingCalculationServiceTest {

    private PricingCalculationService pricingCalculationService;
    private List<TickData> testTicks;

    @BeforeEach
    void setUp() {
        pricingCalculationService = new PricingCalculationService();
        
        // Create test data with different rates for testing
        testTicks = Arrays.asList(
            new TickData("P1", "USD.INR", "SPOT", new BigDecimal("10000"), 
                        new BigDecimal("82.45"), new BigDecimal("82.55"), LocalDateTime.now()),
            new TickData("P1", "USD.INR", "SPOT", new BigDecimal("20000"), 
                        new BigDecimal("82.40"), new BigDecimal("82.60"), LocalDateTime.now()),
            new TickData("P1", "USD.INR", "SPOT", new BigDecimal("30000"), 
                        new BigDecimal("82.50"), new BigDecimal("82.50"), LocalDateTime.now())
        );
    }

    @Test
    void testBestPriceCalculation() {
        PriceCalculationResult result = pricingCalculationService.calculatePrice(
            testTicks, PricingStrategy.BEST_PRICE, "USD.INR", "SPOT", new BigDecimal("10000"));
        
        assertNotNull(result);
        assertEquals(PricingStrategy.BEST_PRICE, result.getStrategy());
        
        // Best bid should be highest: 82.50
        assertEquals(new BigDecimal("82.50"), result.getCalculatedBidRate());
        
        // Best ask should be lowest: 82.50  
        assertEquals(new BigDecimal("82.50"), result.getCalculatedAskRate());
    }

    @Test
    void testWorstPriceCalculation() {
        PriceCalculationResult result = pricingCalculationService.calculatePrice(
            testTicks, PricingStrategy.WORST_PRICE, "USD.INR", "SPOT", new BigDecimal("10000"));
        
        assertNotNull(result);
        assertEquals(PricingStrategy.WORST_PRICE, result.getStrategy());
        
        // Worst bid should be lowest: 82.40
        assertEquals(new BigDecimal("82.40"), result.getCalculatedBidRate());
        
        // Worst ask should be highest: 82.60
        assertEquals(new BigDecimal("82.60"), result.getCalculatedAskRate());
    }

    @Test
    void testVWAPCalculation() {
        PriceCalculationResult result = pricingCalculationService.calculatePrice(
            testTicks, PricingStrategy.VWAP, "USD.INR", "SPOT", new BigDecimal("10000"));
        
        assertNotNull(result);
        assertEquals(PricingStrategy.VWAP, result.getStrategy());
        
        // VWAP Bid = (82.45*10000 + 82.40*20000 + 82.50*30000) / (10000+20000+30000)
        // = (824500 + 1648000 + 2475000) / 60000 = 4947500 / 60000 = 82.458333
        assertEquals(0, result.getCalculatedBidRate().compareTo(new BigDecimal("82.458333")));
        
        // VWAP Ask = (82.55*10000 + 82.60*20000 + 82.50*30000) / 60000  
        // = (825500 + 1652000 + 2475000) / 60000 = 4952500 / 60000 = 82.541667
        assertEquals(0, result.getCalculatedAskRate().compareTo(new BigDecimal("82.541667")));
    }

    @Test
    void testDeepAvgCalculation() {
        PriceCalculationResult result = pricingCalculationService.calculatePrice(
            testTicks, PricingStrategy.DEEP_AVG, "USD.INR", "SPOT", new BigDecimal("10000"));
        
        assertNotNull(result);
        assertEquals(PricingStrategy.DEEP_AVG, result.getStrategy());
        
        // Deep Avg should use all 3 worst prices since we have only 3 ticks
        // For bid (worst = lowest): (82.40 + 82.45 + 82.50) / 3 = 82.45
        assertEquals(0, result.getCalculatedBidRate().compareTo(new BigDecimal("82.450000")));
        
        // For ask (worst = highest): (82.60 + 82.55 + 82.50) / 3 = 82.55  
        assertEquals(0, result.getCalculatedAskRate().compareTo(new BigDecimal("82.550000")));
    }

    @Test
    void testDeepVWAPCalculation() {
        PriceCalculationResult result = pricingCalculationService.calculatePrice(
            testTicks, PricingStrategy.DEEP_VWAP, "USD.INR", "SPOT", new BigDecimal("10000"));
        
        assertNotNull(result);
        assertEquals(PricingStrategy.DEEP_VWAP, result.getStrategy());
        
        // Should have valid calculated rates
        assertNotNull(result.getCalculatedBidRate());
        assertNotNull(result.getCalculatedAskRate());
        assertTrue(result.getCalculatedBidRate().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.getCalculatedAskRate().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testEmptyTickDataList() {
        PriceCalculationResult result = pricingCalculationService.calculatePrice(
            Arrays.asList(), PricingStrategy.BEST_PRICE, "USD.INR", "SPOT", new BigDecimal("10000"));
        
        assertNull(result);
    }

    @Test
    void testNullTickDataList() {
        PriceCalculationResult result = pricingCalculationService.calculatePrice(
            null, PricingStrategy.BEST_PRICE, "USD.INR", "SPOT", new BigDecimal("10000"));
        
        assertNull(result);
    }
}