# Pricer Engine

A comprehensive FX pricing engine that processes Redis tick data and calculates various pricing strategies for multiple currency sources.

## Overview

This pricing engine implements multiple pricing calculation strategies for FX tick data retrieved from Redis. It supports different pricing methodologies for different sources (P1, P2, P3) and provides both bid and ask rate calculations.

## Features

### Pricing Strategies

1. **Best Price** - Most favourable rate per currency per volume
   - For bid: Highest bid rate (better for seller)
   - For ask: Lowest ask rate (better for buyer)

2. **Worst Price** - Least favourable rate per currency per volume
   - For bid: Lowest bid rate (worse for seller)  
   - For ask: Highest ask rate (worse for buyer)

3. **VWAP (Volume Weighted Average Price)**
   - Formula: `VWAP = (Σ Tick Price × Tick Volume) / Σ Tick Volume`

4. **Deep Avg (Last 3 Worst)** - Average of the last three worst prices
   - Formula: `Deep Avg = (Σ of last 3 worst prices) / 3`

5. **Deep VWAP (Last 3 Worst Ticks)** - Volume weighted average for last three worst price ticks
   - Formula: `Deep VWAP = (Σ Last 3 (Tick Price × Tick Volume)) / Σ Last 3 Volumes`

### Source Configuration

- **P1**: Configured for BEST_PRICE strategy
- **P2**: Configured for DEEP_AVG strategy  
- **P3**: Configured for VWAP strategy

## Architecture

### Core Components

1. **Model Classes**
   - `TickData`: Represents individual tick data from Redis
   - `PriceCalculationResult`: Contains calculated pricing results
   - `PricingStrategy`: Enum defining available strategies

2. **Service Classes**
   - `RedisTickDataService`: Handles Redis integration and data retrieval
   - `PricingCalculationService`: Implements all pricing calculation strategies

3. **Configuration**
   - `PricingConfig`: Manages source-to-strategy mappings
   - `RedisConfig`: Redis connection configuration

4. **Scheduler**
   - `PricingScheduler`: Automated pricing calculations (runs every 30 seconds)

## Redis Key Format

The system expects Redis keys in the format:
```
tickbuf:{SOURCE}:{CURRENCY}:{TYPE}:{VOLUME}
```

Example keys from the problem statement:
```
tickbuf:P2:USD.INR:SPOT:20000.0
tickbuf:P1:USD.INR:FORWARD:10000.0
tickbuf:P3:USD.INR:FORWARD:30000.0
```

## Running the Application

### Prerequisites

- Java 11 or higher
- Maven 3.6+
- Redis server (for full functionality)

### Build and Test

```bash
# Build the project
mvn clean compile

# Run tests
mvn test

# Run standalone demo (no Redis required)
mvn compile exec:java -Dexec.mainClass="com.pricer.demo.StandalonePricingDemo"
```

### Sample Output

The demo processes the sample Redis keys and shows pricing calculations:

```
=== PRICING CONFIGURATION ===
P1 -> BEST_PRICE: Best Price - Most favourable rate per currency per volume
P2 -> DEEP_AVG: Deep Avg - Average of last 3 worst prices  
P3 -> VWAP: Volume Weighted Average Price
=== PRICING CALCULATION RESULTS ===
Currency: USD.INR
  Type: SPOT
    P1 [BEST_PRICE]: Vol=1000000.0 | Bid=82.48 | Ask=82.52
    P2 [DEEP_AVG]: Vol=1000000.0 | Bid=82.45 | Ask=82.55
    P3 [VWAP]: Vol=1000000.0 | Bid=82.40 | Ask=82.60
  Type: FORWARD
    P1 [BEST_PRICE]: Vol=20000.0 | Bid=82.53 | Ask=82.57
    P2 [DEEP_AVG]: Vol=20000.0 | Bid=82.50 | Ask=82.60
    P3 [VWAP]: Vol=20000.0 | Bid=82.45 | Ask=82.65
Results by source: P1=8 (BEST_PRICE), P2=8 (DEEP_AVG), P3=8 (VWAP)
```

## Configuration

### Application Properties

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    database: 0

pricing:
  source-strategies:
    P1: BEST_PRICE
    P2: DEEP_AVG  
    P3: VWAP
```
## Testing
The project includes comprehensive unit tests for all pricing strategies:
```bash
mvn test
```

Test coverage includes:
- Best Price calculations
- Worst Price calculations  
- VWAP calculations
- Deep Avg calculations
- Deep VWAP calculations
- Edge cases (empty data, null inputs)

## Implementation Details

### Processing Flow

1. **Data Retrieval**: Fetch tick data from Redis using pattern `tickbuf:*`
2. **Data Grouping**: Group ticks by currency, type, and volume
3. **Source Processing**: Apply configured strategy for each source (P1, P2, P3)
4. **Calculation**: Execute pricing calculations for both bid and ask rates
5. **Results**: Log and return structured pricing results

### Key Features

- **Scalable**: Handles multiple currency pairs and volumes
- **Configurable**: Easy to change source strategies
- **Automated**: Scheduled processing every 30 seconds
- **Testable**: Comprehensive unit test coverage
- **Observable**: Detailed logging for all calculations

## Extension Points

The system is designed for easy extension:

1. **New Strategies**: Add new pricing strategies by extending `PricingStrategy` enum
2. **New Sources**: Add additional sources beyond P1, P2, P3
3. **New Currencies**: Support additional currency pairs
4. **Custom Grouping**: Modify data grouping logic as needed

## Sample Data

The system works with the following sample Redis keys from the problem statement:

- 24 Redis keys covering P1, P2, P3 sources
- USD.INR currency pair  
- SPOT and FORWARD types
- Various volumes: 10K, 20K, 30K, 1M

This comprehensive pricing engine provides a robust foundation for FX rate calculations with multiple configurable strategies.