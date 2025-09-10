# USD/INR Rate Logic Module

This module provides comprehensive functionality for handling USD/INR (US Dollar to Indian Rupee) currency exchange rates within the pricer engine system.

## Features

- **Rate Processing**: Validate, transform, and cache USD/INR exchange rates
- **Multi-Source Support**: Handle rates from various sources (Refinitiv, JP Morgan, FXClear, RBI, etc.)
- **Rate Validation**: Comprehensive validation with configurable bounds and spread checks
- **Caching System**: In-memory caching with configurable TTL (Time To Live)
- **Currency Conversion**: Bidirectional USD ↔ INR conversion with proper bid/ask handling
- **Historical Data**: Maintain rate history and calculate statistics
- **Export Capabilities**: Export rate data in JSON or CSV formats

## Quick Start

### Basic Usage

```python
from usd_inr_rate_logic import process_usd_inr_rate, convert_usd_to_inr, convert_inr_to_usd

# Process a new USD/INR rate
rate_data = {
    "bid": 83.25,
    "ask": 83.30,
    "timestamp": "2024-09-10T21:00:00",
    "source": "refinitiv",
    "volume": 1000000
}

processed_rate = process_usd_inr_rate(rate_data)
if processed_rate:
    print(f"Mid rate: {processed_rate.mid}")

# Convert currencies
usd_amount = 1000
inr_result = convert_usd_to_inr(usd_amount)
print(f"${usd_amount} USD = ₹{inr_result:,.2f} INR")

inr_amount = 100000
usd_result = convert_inr_to_usd(inr_amount)
print(f"₹{inr_amount} INR = ${usd_result:,.2f} USD")
```

### Advanced Usage

```python
from usd_inr_rate_logic import USDINRRateProcessor, RateSource

# Create a processor with custom cache TTL
processor = USDINRRateProcessor(cache_ttl=600)  # 10 minutes

# Process multiple rates
rates = [
    {"bid": 83.20, "ask": 83.30, "source": "refinitiv", "timestamp": "2024-09-10T21:00:00"},
    {"bid": 83.25, "ask": 83.35, "source": "jpmorgan", "timestamp": "2024-09-10T21:01:00"},
]

for rate_data in rates:
    processor.process_rate(rate_data)

# Get statistics
stats = processor.get_rate_statistics(hours=24)
print(f"24h rate statistics: {stats}")

# Export data
json_data = processor.export_rates("json")
csv_data = processor.export_rates("csv")
```

## Rate Sources

The module supports the following rate sources:

- `REFINITIV`: Refinitiv (formerly Thomson Reuters) market data
- `JPMORGAN`: JP Morgan rates
- `FXCLEAR`: FX clearing house rates
- `RBI`: Reserve Bank of India official rates
- `FEDERATED`: Aggregated/federated rates

## Data Structure

### USDINRRate

```python
@dataclass
class USDINRRate:
    bid: float          # Bid price (rate at which market makers buy USD)
    ask: float          # Ask price (rate at which market makers sell USD)  
    mid: float          # Mid price (calculated as (bid + ask) / 2)
    timestamp: datetime # Rate timestamp
    source: RateSource  # Source of the rate
    volume: Optional[float] = None   # Trading volume (if available)
    spread: Optional[float] = None   # Spread (calculated as ask - bid)
```

## Validation Rules

The module enforces the following validation rules:

1. **Rate Bounds**: USD/INR rates must be between 60.0 and 100.0 (configurable)
2. **Bid/Ask Relationship**: Bid rate must be ≤ Ask rate
3. **Spread Limit**: Maximum spread of 2% of mid rate
4. **Timestamp Validity**: Rates must not be older than 24 hours

## Conversion Logic

### USD to INR Conversion
- Uses **ask rate** by default (buying INR with USD)
- Formula: `INR_amount = USD_amount × ask_rate`

### INR to USD Conversion  
- Uses **bid rate** by default (selling INR for USD)
- Formula: `USD_amount = INR_amount ÷ bid_rate`

## Caching Strategy

- **In-Memory Cache**: Stores latest rates from each source
- **TTL Support**: Configurable expiration (default: 5 minutes)
- **Auto-Cleanup**: Expired entries are automatically removed
- **Source Preference**: Can specify preferred source with fallback

## Error Handling

The module provides comprehensive error handling:

- **Validation Errors**: Detailed error messages for invalid rates
- **Missing Data**: Graceful handling of missing rate data
- **Conversion Failures**: Returns `None` for failed conversions with appropriate logging
- **Cache Misses**: Falls back to alternative sources or returns `None`

## Integration with FX Ingestion System

This module is designed to complement the existing Java-based FX ingestion system:

- **Compatible Data Format**: Accepts similar rate structures as Java system
- **Multi-Source Support**: Handles same sources as configured in `application.yml`
- **Logging**: Consistent logging format for monitoring and debugging
- **Extensible**: Easy to add new sources or modify validation rules

## Example Integration

```python
# Example of integrating with incoming FX data
def handle_fx_message(kafka_message):
    """Process incoming FX rate from Kafka message"""
    try:
        rate_data = json.loads(kafka_message.value)
        
        # Filter for USD/INR rates only
        if rate_data.get('symbol') == 'USD/INR':
            processed_rate = process_usd_inr_rate(rate_data)
            if processed_rate:
                # Forward to downstream systems
                publish_to_pricing_engine(processed_rate)
                
    except Exception as e:
        logger.error(f"Error processing FX message: {e}")
```

## Testing

Run the comprehensive test suite:

```bash
python3 test_usd_inr_rate_logic.py
```

The test suite covers:
- Rate validation scenarios
- Caching functionality with TTL
- Currency conversion accuracy
- Statistics calculation
- Export functionality
- Error handling

## Performance Considerations

- **Memory Usage**: Rate history is limited to last 1000 entries
- **Cache Efficiency**: O(1) lookup time for cached rates
- **CPU Usage**: Minimal overhead for validation and calculations
- **Thread Safety**: Not thread-safe by default (use locks if needed in multi-threaded environment)

## Future Enhancements

Potential areas for extension:

1. **Database Integration**: Persist rates to database for historical analysis
2. **Real-time Streaming**: WebSocket integration for live rate feeds  
3. **Rate Alerts**: Configurable alerts for rate movements
4. **Advanced Analytics**: Volatility calculations, trend analysis
5. **API Integration**: REST API endpoints for rate queries
6. **Thread Safety**: Thread-safe operations for concurrent access