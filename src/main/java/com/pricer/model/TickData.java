package com.pricer.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents individual tick data from Redis.
 * Redis key format: tickbuf:{SOURCE}:{CURRENCY}:{TYPE}:{VOLUME}
 */
public class TickData {
    private String source;
    private String currency;
    private String type;
    private Double volume;
    private Double bidRate;
    private Double askRate;
    private LocalDateTime timestamp;

    public TickData() {
    }

    public TickData(String source, String currency, String type, Double volume, 
                    Double bidRate, Double askRate, LocalDateTime timestamp) {
        this.source = source;
        this.currency = currency;
        this.type = type;
        this.volume = volume;
        this.bidRate = bidRate;
        this.askRate = askRate;
        this.timestamp = timestamp;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TickData tickData = (TickData) o;
        return Objects.equals(source, tickData.source) &&
               Objects.equals(currency, tickData.currency) &&
               Objects.equals(type, tickData.type) &&
               Objects.equals(volume, tickData.volume) &&
               Objects.equals(bidRate, tickData.bidRate) &&
               Objects.equals(askRate, tickData.askRate) &&
               Objects.equals(timestamp, tickData.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, currency, type, volume, bidRate, askRate, timestamp);
    }

    @Override
    public String toString() {
        return String.format("TickData{source='%s', currency='%s', type='%s', volume=%.1f, bid=%.2f, ask=%.2f, timestamp=%s}",
                source, currency, type, volume, bidRate, askRate, timestamp);
    }
}