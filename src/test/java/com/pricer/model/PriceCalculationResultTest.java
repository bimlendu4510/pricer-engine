package com.pricer.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PriceCalculationResult model class.
 */
public class PriceCalculationResultTest {

    @Test
    void testPriceCalculationResultCreation() {
        PriceCalculationResult result = new PriceCalculationResult(
                "P1", "USD.INR", "SPOT", 10000.0,
                PricingStrategy.BEST_PRICE, 82.40, 82.60, 5
        );

        assertEquals("P1", result.getSource());
        assertEquals("USD.INR", result.getCurrency());
        assertEquals("SPOT", result.getType());
        assertEquals(10000.0, result.getVolume());
        assertEquals(PricingStrategy.BEST_PRICE, result.getStrategy());
        assertEquals(82.40, result.getBidRate());
        assertEquals(82.60, result.getAskRate());
        assertEquals(5, result.getTickCount());
    }

    @Test
    void testPriceCalculationResultDefaultConstructor() {
        PriceCalculationResult result = new PriceCalculationResult();

        assertNull(result.getSource());
        assertNull(result.getCurrency());
        assertNull(result.getType());
        assertNull(result.getVolume());
        assertNull(result.getStrategy());
        assertNull(result.getBidRate());
        assertNull(result.getAskRate());
        assertEquals(0, result.getTickCount());
    }

    @Test
    void testPriceCalculationResultSetters() {
        PriceCalculationResult result = new PriceCalculationResult();

        result.setSource("P2");
        result.setCurrency("EUR.USD");
        result.setType("FORWARD");
        result.setVolume(20000.0);
        result.setStrategy(PricingStrategy.VWAP);
        result.setBidRate(1.10);
        result.setAskRate(1.12);
        result.setTickCount(8);

        assertEquals("P2", result.getSource());
        assertEquals("EUR.USD", result.getCurrency());
        assertEquals("FORWARD", result.getType());
        assertEquals(20000.0, result.getVolume());
        assertEquals(PricingStrategy.VWAP, result.getStrategy());
        assertEquals(1.10, result.getBidRate());
        assertEquals(1.12, result.getAskRate());
        assertEquals(8, result.getTickCount());
    }

    @Test
    void testPriceCalculationResultEquality() {
        PriceCalculationResult result1 = new PriceCalculationResult(
                "P1", "USD.INR", "SPOT", 10000.0,
                PricingStrategy.BEST_PRICE, 82.40, 82.60, 5
        );
        PriceCalculationResult result2 = new PriceCalculationResult(
                "P1", "USD.INR", "SPOT", 10000.0,
                PricingStrategy.BEST_PRICE, 82.40, 82.60, 5
        );
        PriceCalculationResult result3 = new PriceCalculationResult(
                "P2", "USD.INR", "SPOT", 10000.0,
                PricingStrategy.BEST_PRICE, 82.40, 82.60, 5
        );

        assertEquals(result1, result2);
        assertNotEquals(result1, result3);
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void testPriceCalculationResultToString() {
        PriceCalculationResult result = new PriceCalculationResult(
                "P1", "USD.INR", "SPOT", 10000.0,
                PricingStrategy.BEST_PRICE, 82.40, 82.60, 5
        );

        String toString = result.toString();
        assertTrue(toString.contains("P1"));
        assertTrue(toString.contains("USD.INR"));
        assertTrue(toString.contains("SPOT"));
        assertTrue(toString.contains("10000.0"));
        assertTrue(toString.contains("BEST_PRICE"));
        assertTrue(toString.contains("82.40"));
        assertTrue(toString.contains("82.60"));
        assertTrue(toString.contains("5"));
    }
}