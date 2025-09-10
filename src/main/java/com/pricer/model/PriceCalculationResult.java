package com.pricer.model;

import java.math.BigDecimal;

public class PriceCalculationResult {
    private String source;
    private String currency;
    private String type;
    private BigDecimal volume;
    private PricingStrategy strategy;
    
    private BigDecimal calculatedBidRate;
    private BigDecimal calculatedAskRate;
    private String description;
    
    public PriceCalculationResult() {}
    
    public PriceCalculationResult(String source, String currency, String type, 
                                  BigDecimal volume, PricingStrategy strategy) {
        this.source = source;
        this.currency = currency;
        this.type = type;
        this.volume = volume;
        this.strategy = strategy;
    }

    // Getters and Setters
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }

    public PricingStrategy getStrategy() { return strategy; }
    public void setStrategy(PricingStrategy strategy) { this.strategy = strategy; }

    public BigDecimal getCalculatedBidRate() { return calculatedBidRate; }
    public void setCalculatedBidRate(BigDecimal calculatedBidRate) { this.calculatedBidRate = calculatedBidRate; }

    public BigDecimal getCalculatedAskRate() { return calculatedAskRate; }
    public void setCalculatedAskRate(BigDecimal calculatedAskRate) { this.calculatedAskRate = calculatedAskRate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "PriceCalculationResult{" +
                "source='" + source + '\'' +
                ", currency='" + currency + '\'' +
                ", type='" + type + '\'' +
                ", volume=" + volume +
                ", strategy=" + strategy +
                ", calculatedBidRate=" + calculatedBidRate +
                ", calculatedAskRate=" + calculatedAskRate +
                ", description='" + description + '\'' +
                '}';
    }
}