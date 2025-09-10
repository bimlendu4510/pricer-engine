# pricer-engine

A comprehensive foreign exchange (FX) pricing engine with multi-source data ingestion and specialized USD/INR rate processing.

## Components

### FX Ingestion System (Java)
- **Multi-adapter architecture** supporting WebSocket, REST, and FIX protocol feeds
- **Configurable sources** including Refinitiv, JP Morgan, and FXClear
- **Kafka integration** for real-time data streaming
- **Spring Boot** configuration with YAML-based source management

### USD/INR Rate Logic (Python)
- **Specialized processing** for USD/INR currency pair
- **Rate validation** with configurable bounds and spread checks
- **Multi-source rate caching** with TTL support
- **Bidirectional currency conversion** (USD ↔ INR)
- **Historical data management** and statistics
- **Export capabilities** (JSON/CSV formats)

## Quick Start

### Running the FX Ingestion System
```bash
cd fx-ingestion-system
# Configure sources in resources/application.yml
java Application
```

### Using USD/INR Rate Logic
```python
from usd_inr_rate_logic import process_usd_inr_rate, convert_usd_to_inr

# Process a USD/INR rate
rate_data = {
    "bid": 83.25,
    "ask": 83.30, 
    "source": "refinitiv",
    "timestamp": "2024-09-10T21:00:00"
}

rate = process_usd_inr_rate(rate_data)
inr_amount = convert_usd_to_inr(1000)  # Convert $1000 to INR
```

### Integration Example
```python
# See integration_example.py for complete integration demo
python3 integration_example.py
```

## Testing

```bash
# Test USD/INR rate logic
python3 test_usd_inr_rate_logic.py

# Run integration example
python3 integration_example.py
```

## Documentation

- **[USD/INR Rate Logic](USD_INR_RATE_README.md)** - Detailed documentation for the Python rate processing module
- **[Integration Example](integration_example.py)** - Complete example of Java/Python system integration

## Architecture

The system follows a modular architecture:

1. **Data Ingestion** (Java) - Collects rates from multiple FX sources
2. **Rate Processing** (Python) - Specialized USD/INR validation and transformation  
3. **Integration Layer** - Bridges Java ingestion with Python processing
4. **Downstream Systems** - Pricing engines, risk management, etc.

## Features

✅ **Multi-source FX data ingestion**  
✅ **Real-time rate processing and validation**  
✅ **Configurable rate bounds and spread limits**  
✅ **In-memory caching with TTL**  
✅ **Bidirectional currency conversion**  
✅ **Historical data and statistics**  
✅ **JSON/CSV export capabilities**  
✅ **Comprehensive test coverage**  
✅ **Integration examples and documentation**