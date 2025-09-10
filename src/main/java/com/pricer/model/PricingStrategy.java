package com.pricer.model;

/**
 * Enum defining available pricing calculation strategies.
 */
public enum PricingStrategy {
    BEST_PRICE("Best Price - Most favourable rate per currency per volume"),
    WORST_PRICE("Worst Price - Least favourable rate per currency per volume"),
    VWAP("Volume Weighted Average Price"),
    DEEP_AVG("Deep Avg - Average of last 3 worst prices"),
    DEEP_VWAP("Deep VWAP - Volume weighted average for last 3 worst price ticks");

    private final String description;

    PricingStrategy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name() + ": " + description;
    }
}