#!/usr/bin/env python3
"""
Test script for USD/INR Rate Logic Module

This script tests various functionalities of the USD/INR rate processing system.
"""

import sys
from datetime import datetime, timedelta
from usd_inr_rate_logic import (
    USDINRRate, RateSource, USDINRRateValidator, USDINRRateCache, 
    USDINRRateProcessor, process_usd_inr_rate, get_usd_inr_rate,
    convert_usd_to_inr, convert_inr_to_usd
)


def test_rate_validation():
    """Test rate validation logic"""
    print("Testing rate validation...")
    
    # Valid rate
    valid_rate = USDINRRate(
        bid=83.20, ask=83.30, mid=83.25,
        timestamp=datetime.now(),
        source=RateSource.REFINITIV
    )
    
    is_valid, errors = USDINRRateValidator.validate_rate(valid_rate)
    assert is_valid, f"Valid rate should pass validation, got errors: {errors}"
    print("✓ Valid rate passed validation")
    
    # Invalid rate - bid > ask
    invalid_rate = USDINRRate(
        bid=83.30, ask=83.20, mid=83.25,
        timestamp=datetime.now(),
        source=RateSource.REFINITIV
    )
    
    is_valid, errors = USDINRRateValidator.validate_rate(invalid_rate)
    assert not is_valid, "Invalid rate (bid > ask) should fail validation"
    assert any("cannot be higher than ask" in error for error in errors)
    print("✓ Invalid rate (bid > ask) correctly failed validation")
    
    # Rate out of bounds
    out_of_bounds_rate = USDINRRate(
        bid=150.0, ask=155.0, mid=152.5,
        timestamp=datetime.now(),
        source=RateSource.REFINITIV
    )
    
    is_valid, errors = USDINRRateValidator.validate_rate(out_of_bounds_rate)
    assert not is_valid, "Out of bounds rate should fail validation"
    print("✓ Out of bounds rate correctly failed validation")


def test_rate_cache():
    """Test rate caching functionality"""
    print("\nTesting rate cache...")
    
    cache = USDINRRateCache(ttl_seconds=2)  # 2 second TTL for testing
    
    # Create test rate
    test_rate = USDINRRate(
        bid=83.20, ask=83.30, mid=83.25,
        timestamp=datetime.now(),
        source=RateSource.REFINITIV
    )
    
    # Cache the rate
    cache.set_rate(test_rate)
    
    # Retrieve the rate
    cached_rate = cache.get_rate(RateSource.REFINITIV)
    assert cached_rate is not None, "Should retrieve cached rate"
    assert cached_rate.bid == test_rate.bid, "Cached rate should match original"
    print("✓ Rate caching and retrieval works")
    
    # Test TTL expiration
    import time
    time.sleep(3)  # Wait for TTL to expire
    
    expired_rate = cache.get_rate(RateSource.REFINITIV)
    assert expired_rate is None, "Expired rate should not be retrievable"
    print("✓ TTL expiration works correctly")


def test_rate_processing():
    """Test rate processing functionality"""
    print("\nTesting rate processing...")
    
    processor = USDINRRateProcessor()
    
    # Test valid rate processing
    valid_rate_data = {
        "bid": 83.25,
        "ask": 83.30,
        "timestamp": datetime.now().isoformat(),
        "source": "refinitiv",
        "volume": 1000000
    }
    
    processed_rate = processor.process_rate(valid_rate_data)
    assert processed_rate is not None, "Valid rate should be processed successfully"
    assert processed_rate.bid == 83.25, "Processed rate should maintain bid value"
    print("✓ Valid rate processing works")
    
    # Test invalid rate processing
    invalid_rate_data = {
        "bid": 83.30,  # bid > ask
        "ask": 83.20,
        "timestamp": datetime.now().isoformat(),
        "source": "refinitiv"
    }
    
    processed_invalid = processor.process_rate(invalid_rate_data)
    assert processed_invalid is None, "Invalid rate should not be processed"
    print("✓ Invalid rate rejection works")


def test_currency_conversion():
    """Test currency conversion functionality"""
    print("\nTesting currency conversion...")
    
    # First add a valid rate
    test_rate_data = {
        "bid": 83.20,
        "ask": 83.30,
        "timestamp": datetime.now().isoformat(),
        "source": "refinitiv"
    }
    
    processed_rate = process_usd_inr_rate(test_rate_data)
    assert processed_rate is not None, "Should process rate for conversion tests"
    
    # Test USD to INR conversion
    usd_amount = 1000
    inr_result = convert_usd_to_inr(usd_amount)
    
    assert inr_result is not None, "USD to INR conversion should work"
    expected_inr = usd_amount * 83.30  # Using ask rate
    assert abs(inr_result - expected_inr) < 0.01, f"Expected ~{expected_inr}, got {inr_result}"
    print(f"✓ USD to INR conversion: ${usd_amount} = ₹{inr_result:,.2f}")
    
    # Test INR to USD conversion
    inr_amount = 100000
    usd_result = convert_inr_to_usd(inr_amount)
    
    assert usd_result is not None, "INR to USD conversion should work"
    expected_usd = inr_amount / 83.20  # Using bid rate
    assert abs(usd_result - expected_usd) < 0.01, f"Expected ~{expected_usd}, got {usd_result}"
    print(f"✓ INR to USD conversion: ₹{inr_amount} = ${usd_result:,.2f}")


def test_statistics():
    """Test rate statistics functionality"""
    print("\nTesting rate statistics...")
    
    processor = USDINRRateProcessor()
    
    # Add multiple rates for statistics
    rates_data = [
        {"bid": 83.20, "ask": 83.30, "timestamp": datetime.now().isoformat(), "source": "refinitiv"},
        {"bid": 83.25, "ask": 83.35, "timestamp": datetime.now().isoformat(), "source": "jpmorgan"},
        {"bid": 83.15, "ask": 83.25, "timestamp": datetime.now().isoformat(), "source": "fxclear"}
    ]
    
    for rate_data in rates_data:
        processor.process_rate(rate_data)
    
    stats = processor.get_rate_statistics(hours=1)
    assert "mid_rate" in stats, "Statistics should include mid_rate information"
    assert "rate_count" in stats, "Statistics should include rate count"
    assert stats["rate_count"] == 3, f"Expected 3 rates, got {stats['rate_count']}"
    print("✓ Rate statistics calculation works")
    print(f"  - Rate count: {stats['rate_count']}")
    print(f"  - Current mid rate: {stats['mid_rate']['current']}")
    print(f"  - Rate range: {stats['mid_rate']['low']} - {stats['mid_rate']['high']}")


def test_export_functionality():
    """Test rate export functionality"""
    print("\nTesting export functionality...")
    
    processor = USDINRRateProcessor()
    
    # Add a test rate
    rate_data = {
        "bid": 83.25,
        "ask": 83.30,
        "timestamp": datetime.now().isoformat(),
        "source": "refinitiv"
    }
    
    processor.process_rate(rate_data)
    
    # Test JSON export
    json_export = processor.export_rates("json")
    assert len(json_export) > 0, "JSON export should not be empty"
    assert '"bid": 83.25' in json_export, "JSON should contain rate data"
    print("✓ JSON export works")
    
    # Test CSV export
    csv_export = processor.export_rates("csv")
    assert len(csv_export) > 0, "CSV export should not be empty"
    assert "timestamp,source,bid,ask" in csv_export, "CSV should have proper header"
    assert "83.25,83.3" in csv_export, "CSV should contain rate data"
    print("✓ CSV export works")


def run_all_tests():
    """Run all test functions"""
    print("Running USD/INR Rate Logic Tests")
    print("=" * 50)
    
    try:
        test_rate_validation()
        test_rate_cache()
        test_rate_processing()
        test_currency_conversion()
        test_statistics()
        test_export_functionality()
        
        print("\n" + "=" * 50)
        print("✅ All tests passed successfully!")
        return True
        
    except Exception as e:
        print(f"\n❌ Test failed: {e}")
        import traceback
        traceback.print_exc()
        return False


if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)