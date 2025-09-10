#!/usr/bin/env python3
"""
Integration Example: USD/INR Rate Logic with FX Ingestion System

This example demonstrates how the USD/INR rate logic can integrate with the
existing Java-based FX ingestion system.
"""

import json
from datetime import datetime, timedelta
from typing import Dict, Any
from usd_inr_rate_logic import USDINRRateProcessor, RateSource


class FXIngestionIntegration:
    """
    Integration layer between FX ingestion system and USD/INR rate processing
    """
    
    def __init__(self):
        self.usd_inr_processor = USDINRRateProcessor()
        self.processed_count = 0
    
    def process_fx_message(self, message: Dict[str, Any]) -> bool:
        """
        Process incoming FX message from Java system
        
        Args:
            message: FX message dictionary (typically from Kafka/REST)
            
        Returns:
            True if message was processed successfully
        """
        try:
            # Extract currency pair - only process USD/INR
            symbol = message.get('symbol', '').upper()
            if symbol not in ['USD/INR', 'USDINR', 'USD-INR']:
                return False  # Not a USD/INR rate
            
            # Map Java system fields to Python module format
            rate_data = {
                'bid': float(message.get('bid', 0)),
                'ask': float(message.get('ask', 0)),
                'timestamp': message.get('timestamp', datetime.now().isoformat()),
                'source': self._map_source(message.get('source', 'unknown')),
                'volume': message.get('volume')
            }
            
            # Process the rate
            processed_rate = self.usd_inr_processor.process_rate(rate_data)
            if processed_rate:
                self.processed_count += 1
                
                # Example: Forward to downstream systems
                self._forward_to_pricing_engine(processed_rate)
                return True
                
        except Exception as e:
            print(f"Error processing FX message: {e}")
            
        return False
    
    def _map_source(self, source_name: str) -> str:
        """Map Java system source names to Python enum values"""
        source_mapping = {
            'refinitiv': 'refinitiv',
            'reuters': 'refinitiv',
            'jpmorgan': 'jpmorgan',
            'jp-morgan': 'jpmorgan',
            'fxclear': 'fxclear',
            'ccil': 'fxclear',
            'rbi': 'rbi',
            'reserve-bank': 'rbi'
        }
        
        return source_mapping.get(source_name.lower(), 'refinitiv')
    
    def _forward_to_pricing_engine(self, rate):
        """Example of forwarding processed rate to pricing engine"""
        # In real implementation, this would:
        # - Publish to Kafka topic
        # - Call REST API
        # - Update database
        # - Trigger pricing calculations
        
        print(f"→ Forwarding USD/INR rate {rate.mid} from {rate.source.value} to pricing engine")
    
    def get_statistics(self) -> Dict:
        """Get processing statistics"""
        return {
            'processed_messages': self.processed_count,
            'rate_statistics': self.usd_inr_processor.get_rate_statistics(hours=1),
            'cached_sources': list(self.usd_inr_processor.cache.cache.keys())
        }


def simulate_fx_ingestion():
    """
    Simulate the FX ingestion system sending USD/INR rates
    """
    print("USD/INR Rate Logic Integration Example")
    print("=" * 50)
    
    integration = FXIngestionIntegration()
    
    # Simulate incoming messages from different sources (like the Java adapters)
    current_time = datetime.now()
    sample_messages = [
        {
            'symbol': 'USD/INR',
            'bid': 83.20,
            'ask': 83.30,
            'source': 'refinitiv',
            'timestamp': current_time.isoformat(),
            'volume': 1500000
        },
        {
            'symbol': 'USD/INR', 
            'bid': 83.25,
            'ask': 83.35,
            'source': 'jpmorgan',
            'timestamp': (current_time + timedelta(seconds=60)).isoformat(),
            'volume': 800000
        },
        {
            'symbol': 'EUR/USD',  # This should be ignored
            'bid': 1.0985,
            'ask': 1.0990,
            'source': 'refinitiv',
            'timestamp': (current_time + timedelta(seconds=90)).isoformat()
        },
        {
            'symbol': 'USD/INR',
            'bid': 83.15,
            'ask': 83.25,
            'source': 'fxclear',
            'timestamp': (current_time + timedelta(seconds=120)).isoformat(),
            'volume': 2000000
        }
    ]
    
    # Process messages
    print("Processing incoming FX messages...")
    for i, message in enumerate(sample_messages, 1):
        print(f"\nMessage {i}: {message['symbol']} from {message['source']}")
        success = integration.process_fx_message(message)
        print(f"Status: {'✓ Processed' if success else '✗ Skipped/Failed'}")
    
    # Show statistics
    print(f"\n{'-'*50}")
    print("Processing Statistics:")
    stats = integration.get_statistics()
    print(f"Messages processed: {stats['processed_messages']}")
    
    rate_stats = stats['rate_statistics']
    if 'mid_rate' in rate_stats:
        print(f"Current USD/INR rate: {rate_stats['mid_rate']['current']}")
        print(f"Rate range: {rate_stats['mid_rate']['low']} - {rate_stats['mid_rate']['high']}")
        print(f"Sources active: {', '.join(rate_stats['sources'])}")
    
    # Demonstrate currency conversion
    print(f"\n{'-'*50}")
    print("Currency Conversion Examples:")
    
    # Get current rate for conversion demo
    current_rate = integration.usd_inr_processor.get_current_rate()
    if current_rate:
        print(f"Using rate: {current_rate.bid}/{current_rate.ask} from {current_rate.source.value}")
        
        # Convert $10,000 USD to INR
        usd_amount = 10000
        inr_result = integration.usd_inr_processor.convert_usd_to_inr(usd_amount)
        if inr_result:
            print(f"${usd_amount:,} USD → ₹{inr_result:,.2f} INR")
        
        # Convert ₹500,000 INR to USD
        inr_amount = 500000
        usd_result = integration.usd_inr_processor.convert_inr_to_usd(inr_amount)
        if usd_result:
            print(f"₹{inr_amount:,} INR → ${usd_result:,.2f} USD")
    
    print(f"\n{'-'*50}")
    print("Integration example completed successfully!")


if __name__ == "__main__":
    simulate_fx_ingestion()