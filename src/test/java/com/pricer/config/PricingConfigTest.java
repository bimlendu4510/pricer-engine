package com.pricer.config;

import com.pricer.model.PricingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PricingConfig class.
 */
public class PricingConfigTest {

    private PricingConfig pricingConfig;

    @BeforeEach
    void setUp() {
        pricingConfig = new PricingConfig();
    }

    @Test
    void testDefaultConfiguration() {
        assertEquals(PricingStrategy.BEST_PRICE, pricingConfig.getStrategyForSource("P1"));
        assertEquals(PricingStrategy.DEEP_AVG, pricingConfig.getStrategyForSource("P2"));
        assertEquals(PricingStrategy.VWAP, pricingConfig.getStrategyForSource("P3"));
    }

    @Test
    void testGetStrategyForUnknownSource() {
        assertNull(pricingConfig.getStrategyForSource("P4"));
        assertNull(pricingConfig.getStrategyForSource("UNKNOWN"));
    }

    @Test
    void testSetStrategyForSource() {
        pricingConfig.setStrategyForSource("P4", PricingStrategy.WORST_PRICE);
        assertEquals(PricingStrategy.WORST_PRICE, pricingConfig.getStrategyForSource("P4"));
    }

    @Test
    void testOverrideExistingStrategy() {
        pricingConfig.setStrategyForSource("P1", PricingStrategy.VWAP);
        assertEquals(PricingStrategy.VWAP, pricingConfig.getStrategyForSource("P1"));
    }

    @Test
    void testSourceStrategiesMapNotNull() {
        assertNotNull(pricingConfig.getSourceStrategies());
        assertTrue(pricingConfig.getSourceStrategies().size() >= 3);
    }

    @Test
    void testSetSourceStrategiesMap() {
        java.util.Map<String, PricingStrategy> newStrategies = new java.util.HashMap<>();
        newStrategies.put("P1", PricingStrategy.WORST_PRICE);
        newStrategies.put("P2", PricingStrategy.VWAP);
        
        pricingConfig.setSourceStrategies(newStrategies);
        
        assertEquals(PricingStrategy.WORST_PRICE, pricingConfig.getStrategyForSource("P1"));
        assertEquals(PricingStrategy.VWAP, pricingConfig.getStrategyForSource("P2"));
        assertNull(pricingConfig.getStrategyForSource("P3")); // No longer configured
    }
}