package com.pricer.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TickData {
    private String source;      // P1, P2, P3
    private String currency;    // USD.INR
    private String type;        // SPOT, FORWARD
    private BigDecimal volume;
    private BigDecimal bidRate;
    private BigDecimal askRate;
    private LocalDateTime timestamp;

    public TickData() {}

    public TickData(String source, String currency, String type, BigDecimal volume, 
                    BigDecimal bidRate, BigDecimal askRate, LocalDateTime timestamp) {
        this.source = source;
        this.currency = currency;
        this.type = type;
        this.volume = volume;
        this.bidRate = bidRate;
        this.askRate = askRate;
        this.timestamp = timestamp;
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

    public BigDecimal getBidRate() { return bidRate; }
    public void setBidRate(BigDecimal bidRate) { this.bidRate = bidRate; }

    public BigDecimal getAskRate() { return askRate; }
    public void setAskRate(BigDecimal askRate) { this.askRate = askRate; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "TickData{" +
                "source='" + source + '\'' +
                ", currency='" + currency + '\'' +
                ", type='" + type + '\'' +
                ", volume=" + volume +
                ", bidRate=" + bidRate +
                ", askRate=" + askRate +
                ", timestamp=" + timestamp +
                '}';
    }
}