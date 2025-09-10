package com.pricer.service;

import com.pricer.config.PricingConfig;
import com.pricer.model.PriceCalculationResult;
import com.pricer.model.PricingStrategy;
import com.pricer.model.TickData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Comprehensive unit tests for PricingCalculationService.
 */
public class PricingCalculationServiceTest {

    @Mock
    private PricingConfig pricingConfig;

    @InjectMocks
    private PricingCalculationService pricingCalculationService;

    private List<TickData> sampleTickData;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        setupSampleData();
        setupMockConfig();
    }

    private void setupSampleData() {
        sampleTickData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Create test data for USD.INR SPOT with 10000.0 volume
        // P1 ticks - for BEST_PRICE strategy
        sampleTickData.add(new TickData("P1", "USD.INR", "SPOT", 10000.0, 82.40, 82.60, now));
        sampleTickData.add(new TickData("P1", "USD.INR", "SPOT", 10000.0, 82.45, 82.55, now.minusMinutes(1)));
        sampleTickData.add(new TickData("P1", "USD.INR", "SPOT", 10000.0, 82.50, 82.65, now.minusMinutes(2)));

        // P2 ticks - for DEEP_AVG strategy
        sampleTickData.add(new TickData("P2", "USD.INR", "SPOT", 10000.0, 82.35, 82.70, now));
        sampleTickData.add(new TickData("P2", "USD.INR", "SPOT", 10000.0, 82.30, 82.75, now.minusMinutes(1)));
        sampleTickData.add(new TickData("P2", "USD.INR", "SPOT", 10000.0, 82.25, 82.80, now.minusMinutes(2)));
        sampleTickData.add(new TickData("P2", "USD.INR", "SPOT", 10000.0, 82.20, 82.85, now.minusMinutes(3)));

        // P3 ticks - for VWAP strategy with different volumes for volume weighting
        sampleTickData.add(new TickData("P3", "USD.INR", "SPOT", 10000.0, 82.40, 82.60, now));
        sampleTickData.add(new TickData("P3", "USD.INR", "SPOT", 10000.0, 82.50, 82.70, now.minusMinutes(1)));
    }

    private void setupMockConfig() {
        when(pricingConfig.getStrategyForSource("P1")).thenReturn(PricingStrategy.BEST_PRICE);
        when(pricingConfig.getStrategyForSource("P2")).thenReturn(PricingStrategy.DEEP_AVG);
        when(pricingConfig.getStrategyForSource("P3")).thenReturn(PricingStrategy.VWAP);
        when(pricingConfig.getStrategyForSource("P4")).thenReturn(PricingStrategy.WORST_PRICE);
        when(pricingConfig.getStrategyForSource("P5")).thenReturn(PricingStrategy.DEEP_VWAP);
    }

    @Test
    void testCalculatePricing_WithValidData() {
        List<PriceCalculationResult> results = pricingCalculationService.calculatePricing(sampleTickData);

        assertNotNull(results);
        assertEquals(3, results.size()); // P1, P2, P3

        // Verify each source has results
        assertTrue(results.stream().anyMatch(r -> r.getSource().equals("P1")));
        assertTrue(results.stream().anyMatch(r -> r.getSource().equals("P2")));
        assertTrue(results.stream().anyMatch(r -> r.getSource().equals("P3")));
    }

    @Test
    void testCalculatePricing_WithEmptyData() {
        List<PriceCalculationResult> results = pricingCalculationService.calculatePricing(new ArrayList<>());

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testCalculatePricing_WithNullData() {
        List<PriceCalculationResult> results = pricingCalculationService.calculatePricing(null);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testBestPriceStrategy() {
        List<TickData> p1Data = sampleTickData.stream()
                .filter(tick -> "P1".equals(tick.getSource()))
                .toList();

        List<PriceCalculationResult> results = pricingCalculationService.calculatePricing(p1Data);

        assertNotNull(results);
        assertEquals(1, results.size());

        PriceCalculationResult result = results.get(0);
        assertEquals("P1", result.getSource());
        assertEquals(PricingStrategy.BEST_PRICE, result.getStrategy());
        
        // Best bid should be highest: 82.50
        assertEquals(82.50, result.getBidRate(), 0.001);
        // Best ask should be lowest: 82.55
        assertEquals(82.55, result.getAskRate(), 0.001);
    }

    @Test
    void testDeepAvgStrategy() {
        List<TickData> p2Data = sampleTickData.stream()
                .filter(tick -> "P2".equals(tick.getSource()))
                .toList();

        List<PriceCalculationResult> results = pricingCalculationService.calculatePricing(p2Data);

        assertNotNull(results);
        assertEquals(1, results.size());

        PriceCalculationResult result = results.get(0);
        assertEquals("P2", result.getSource());
        assertEquals(PricingStrategy.DEEP_AVG, result.getStrategy());
        
        // Deep avg of last 3 worst bids: we have 4 bids sorted: [82.20, 82.25, 82.30, 82.35]
        // Last 3 are: (82.25 + 82.30 + 82.35) / 3 = 82.30
        assertEquals(82.30, result.getBidRate(), 0.001);
        // Deep avg of last 3 worst asks: we have 4 asks sorted desc: [82.85, 82.80, 82.75, 82.70]
        // Last 3 are: (82.80 + 82.75 + 82.70) / 3 = 82.75
        assertEquals(82.75, result.getAskRate(), 0.001);
    }

    @Test
    void testVWAPStrategy() {
        List<TickData> p3Data = sampleTickData.stream()
                .filter(tick -> "P3".equals(tick.getSource()))
                .toList();

        List<PriceCalculationResult> results = pricingCalculationService.calculatePricing(p3Data);

        assertNotNull(results);
        assertEquals(1, results.size());

        PriceCalculationResult result = results.get(0);
        assertEquals("P3", result.getSource());
        assertEquals(PricingStrategy.VWAP, result.getStrategy());
        
        // VWAP bid: (82.40*10000 + 82.50*10000) / (10000 + 10000) = 82.45
        assertEquals(82.45, result.getBidRate(), 0.001);
        // VWAP ask: (82.60*10000 + 82.70*10000) / (10000 + 10000) = 82.65
        assertEquals(82.65, result.getAskRate(), 0.001);
    }

    @Test
    void testWorstPriceStrategy() {
        // Create test data for WORST_PRICE strategy
        List<TickData> testData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        testData.add(new TickData("P4", "USD.INR", "SPOT", 10000.0, 82.40, 82.60, now));
        testData.add(new TickData("P4", "USD.INR", "SPOT", 10000.0, 82.45, 82.55, now.minusMinutes(1)));
        testData.add(new TickData("P4", "USD.INR", "SPOT", 10000.0, 82.50, 82.65, now.minusMinutes(2)));

        List<PriceCalculationResult> results = pricingCalculationService.calculatePricing(testData);

        assertNotNull(results);
        assertEquals(1, results.size());

        PriceCalculationResult result = results.get(0);
        assertEquals("P4", result.getSource());
        assertEquals(PricingStrategy.WORST_PRICE, result.getStrategy());
        
        // Worst bid should be lowest: 82.40
        assertEquals(82.40, result.getBidRate(), 0.001);
        // Worst ask should be highest: 82.65
        assertEquals(82.65, result.getAskRate(), 0.001);
    }

    @Test
    void testDeepVWAPStrategy() {
        // Create test data for DEEP_VWAP strategy
        List<TickData> testData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Create data with SAME volume but different rates for DEEP_VWAP calculation
        testData.add(new TickData("P5", "USD.INR", "SPOT", 10000.0, 82.20, 82.80, now)); // Worst bid, worst ask
        testData.add(new TickData("P5", "USD.INR", "SPOT", 10000.0, 82.30, 82.70, now.minusMinutes(1))); // Second worst
        testData.add(new TickData("P5", "USD.INR", "SPOT", 10000.0, 82.40, 82.60, now.minusMinutes(2))); // Third worst
        testData.add(new TickData("P5", "USD.INR", "SPOT", 10000.0, 82.50, 82.50, now.minusMinutes(3))); // Best

        List<PriceCalculationResult> results = pricingCalculationService.calculatePricing(testData);

        assertNotNull(results);
        assertEquals(1, results.size());

        PriceCalculationResult result = results.get(0);
        assertEquals("P5", result.getSource());
        assertEquals(PricingStrategy.DEEP_VWAP, result.getStrategy());
        
        // Deep VWAP calculations will use the last N ticks after sorting, verify basic functionality
        assertNotNull(result.getBidRate());
        assertNotNull(result.getAskRate());
        assertTrue(result.getBidRate() > 0);
        assertTrue(result.getAskRate() > 0);
    }

    @Test
    void testMultipleCurrencyTypes() {
        List<TickData> multiData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // USD.INR SPOT
        multiData.add(new TickData("P1", "USD.INR", "SPOT", 10000.0, 82.40, 82.60, now));
        // USD.INR FORWARD
        multiData.add(new TickData("P1", "USD.INR", "FORWARD", 10000.0, 82.45, 82.65, now));
        // EUR.USD SPOT
        multiData.add(new TickData("P1", "EUR.USD", "SPOT", 10000.0, 1.10, 1.12, now));

        List<PriceCalculationResult> results = pricingCalculationService.calculatePricing(multiData);

        assertNotNull(results);
        assertEquals(3, results.size()); // Three different groups

        // Check that we have results for different currencies and types
        assertTrue(results.stream().anyMatch(r -> "USD.INR".equals(r.getCurrency()) && "SPOT".equals(r.getType())));
        assertTrue(results.stream().anyMatch(r -> "USD.INR".equals(r.getCurrency()) && "FORWARD".equals(r.getType())));
        assertTrue(results.stream().anyMatch(r -> "EUR.USD".equals(r.getCurrency()) && "SPOT".equals(r.getType())));
    }

    @Test
    void testTickDataWithMissingRates() {
        List<TickData> testData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Normal tick
        testData.add(new TickData("P1", "USD.INR", "SPOT", 10000.0, 82.40, 82.60, now));
        // Tick with null bid
        testData.add(new TickData("P1", "USD.INR", "SPOT", 10000.0, null, 82.65, now.minusMinutes(1)));
        // Tick with null ask
        testData.add(new TickData("P1", "USD.INR", "SPOT", 10000.0, 82.45, null, now.minusMinutes(2)));

        List<PriceCalculationResult> results = pricingCalculationService.calculatePricing(testData);

        assertNotNull(results);
        assertEquals(1, results.size());

        PriceCalculationResult result = results.get(0);
        // Should still calculate using available data
        assertNotNull(result.getBidRate());
        assertNotNull(result.getAskRate());
    }
}