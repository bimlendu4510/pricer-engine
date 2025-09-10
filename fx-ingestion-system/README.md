# USD-INR Rate Processing Engine

This module implements the business logic for processing USD-INR foreign exchange rates according to specific market conditions and rules.

## Overview

The USD-INR rate processor handles 4 main conditions for rate processing:

1. **Condition 0**: Half paisa spread (0.5 paisa) - rates remain unchanged
2. **Condition 1**: Large spread (>=2 paisa) - rates remain unchanged  
3. **Condition 2**: Price movement beyond last traded price
4. **Condition 3**: Trending market based on Primac values
5. **Condition 4**: Weighted average fallback calculation

## Key Components

### StandardMarketRate
Enhanced model class containing:
- `bid`, `ask` - current market prices
- `lastTradedPrice` - reference price for comparison
- `primac1`, `primac2`, `primac3` - market indicator values
- `symbol`, `timestamp`, `source` - metadata

### UsdInrRateProcessor
Core processing engine implementing all business logic conditions:
```java
UsdInrRateProcessor processor = new UsdInrRateProcessor();
StandardMarketRate result = processor.processUsdInrRate(inputRate);
```

### UsdInrRateService  
Integration service for the FX ingestion system:
```java
UsdInrRateService service = new UsdInrRateService();
StandardMarketRate processed = service.processAndPublishRate(incomingRate);
```

## Business Rules

### Spread Thresholds
- **0.5 paisa**: Exact match - no processing needed
- **>= 2 paisa**: Large spread - no processing needed
- **1-2 paisa**: Apply 1 paisa spread (0.5 each side)
- **0-1 paisa**: Apply 0.5 paisa spread

### Condition Details

#### Condition 2a: Bid < Ask <= Last Traded Price
```
Example: bid=83, ask=84, lastPrice=85
Result: ask=84 (unchanged), bid=84-spread
```

#### Condition 2b: Last Traded Price <= Bid < Ask  
```
Example: lastPrice=83, bid=84, ask=85
Result: bid=84 (unchanged), ask=84+spread
```

#### Condition 3: Trending Market
- **Upward trend**: P3 > P2 > P1 → Mid = P1 + spread
- **Downward trend**: P3 < P2 < P1 → Mid = P1 + spread

#### Condition 4: Weighted Average
```
Mid = P1*0.8 + P2*0.2 + P3*0.05
Then apply appropriate spread around Mid
```

## Usage Examples

### Basic Processing
```java
// Create rate
StandardMarketRate rate = new StandardMarketRate("USD-INR", 
    new BigDecimal("83.1000"), new BigDecimal("83.1080"));

// Process rate
UsdInrRateProcessor processor = new UsdInrRateProcessor();
StandardMarketRate result = processor.processUsdInrRate(rate);
```

### With Service Integration
```java
// Using service layer
UsdInrRateService service = new UsdInrRateService();
StandardMarketRate processed = service.processAndPublishRate(incomingRate);
```

## Testing

### Run Unit Tests
```bash
javac -cp . model/*.java test/model/*.java
java -cp .:test test.model.UsdInrRateProcessorTest
```

### Run Demo
```bash  
javac -cp . demo/*.java
java -cp .:demo demo.UsdInrProcessorDemo
```

## Integration with FX Ingestion System

The USD-INR processor integrates with existing system components:

1. **Market Feed Adaptors** → receive raw rates → **UsdInrRateService** → **Kafka Producer**
2. **Configuration** via `application.yml` for enabling/disabling processing
3. **Registry** pattern for managing multiple currency processors

## Configuration Example

```yaml
fx:
  processing:
    usd-inr:
      enabled: true
      conditions:
        - condition0: true
        - condition1: true  
        - condition2: true
        - condition3: true
        - condition4: true
```

## Error Handling

The processor includes validation for:
- Null rate objects
- Missing bid/ask values
- Invalid currency symbols
- Calculation errors

All errors are logged and propagated appropriately for system monitoring.