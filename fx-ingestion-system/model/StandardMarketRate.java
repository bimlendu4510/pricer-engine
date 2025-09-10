package model;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class StandardMarketRate {
    private String symbol;
    private BigDecimal bid;
    private BigDecimal ask;
    private BigDecimal lastTradedPrice;
    private BigDecimal primac1;
    private BigDecimal primac2;
    private BigDecimal primac3;
    private LocalDateTime timestamp;
    private String source;
    
    public StandardMarketRate() {}
    
    public StandardMarketRate(String symbol, BigDecimal bid, BigDecimal ask) {
        this.symbol = symbol;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public BigDecimal getBid() { return bid; }
    public void setBid(BigDecimal bid) { this.bid = bid; }
    
    public BigDecimal getAsk() { return ask; }
    public void setAsk(BigDecimal ask) { this.ask = ask; }
    
    public BigDecimal getLastTradedPrice() { return lastTradedPrice; }
    public void setLastTradedPrice(BigDecimal lastTradedPrice) { this.lastTradedPrice = lastTradedPrice; }
    
    public BigDecimal getPrimac1() { return primac1; }
    public void setPrimac1(BigDecimal primac1) { this.primac1 = primac1; }
    
    public BigDecimal getPrimac2() { return primac2; }
    public void setPrimac2(BigDecimal primac2) { this.primac2 = primac2; }
    
    public BigDecimal getPrimac3() { return primac3; }
    public void setPrimac3(BigDecimal primac3) { this.primac3 = primac3; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    // Utility method to calculate spread in paisa (1 rupee = 100 paisa)
    public BigDecimal getSpreadInPaisa() {
        if (bid != null && ask != null) {
            return ask.subtract(bid).multiply(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }
    
    @Override
    public String toString() {
        return String.format("StandardMarketRate{symbol='%s', bid=%s, ask=%s, lastTradedPrice=%s, spread=%.2f paisa}", 
                           symbol, bid, ask, lastTradedPrice, getSpreadInPaisa());
    }
}
