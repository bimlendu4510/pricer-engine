package com.pricer.model;

import java.util.Objects;

/**
 * Contains calculated pricing results for a specific source, currency, type and volume.
 */
public class PriceCalculationResult {
    private String source;
    private String currency;
    private String type;
    private Double volume;
    private PricingStrategy strategy;
    private Double bidRate;
    private Double askRate;
    private int tickCount;

    public PriceCalculationResult() {
    }

    public PriceCalculationResult(String source, String currency, String type, Double volume,
                                  PricingStrategy strategy, Double bidRate, Double askRate, int tickCount) {
        this.source = source;
        this.currency = currency;
        this.type = type;
        this.volume = volume;
        this.strategy = strategy;
        this.bidRate = bidRate;
        this.askRate = askRate;
        this.tickCount = tickCount;
    }

    // Getters and setters
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }

    public PricingStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(PricingStrategy strategy) {
        this.strategy = strategy;
    }

    public Double getBidRate() {
        return bidRate;
    }

    public void setBidRate(Double bidRate) {
        this.bidRate = bidRate;
    }

    public Double getAskRate() {
        return askRate;
    }

    public void setAskRate(Double askRate) {
        this.askRate = askRate;
    }

    public int getTickCount() {
        return tickCount;
    }

    public void setTickCount(int tickCount) {
        this.tickCount = tickCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceCalculationResult that = (PriceCalculationResult) o;
        return tickCount == that.tickCount &&
               Objects.equals(source, that.source) &&
               Objects.equals(currency, that.currency) &&
               Objects.equals(type, that.type) &&
               Objects.equals(volume, that.volume) &&
               strategy == that.strategy &&
               Objects.equals(bidRate, that.bidRate) &&
               Objects.equals(askRate, that.askRate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, currency, type, volume, strategy, bidRate, askRate, tickCount);
    }

    @Override
    public String toString() {
        return String.format("PriceCalculationResult{source='%s', currency='%s', type='%s', volume=%.1f, " +
                           "strategy=%s, bid=%.2f, ask=%.2f, ticks=%d}",
                           source, currency, type, volume, strategy, bidRate, askRate, tickCount);
    }
}