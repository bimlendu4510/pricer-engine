"""
USD/INR Rate Logic Module

This module provides comprehensive functionality for handling USD/INR currency exchange rates
including rate validation, transformation, caching, and historical data management.
"""

from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple, Union
import json
import logging
from dataclasses import dataclass
from enum import Enum


# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class RateSource(Enum):
    """Enumeration of supported rate sources"""
    REFINITIV = "refinitiv"
    FXCLEAR = "fxclear"
    JPMORGAN = "jpmorgan"
    RBI = "rbi"  # Reserve Bank of India
    FEDERATED = "federated"


@dataclass
class USDINRRate:
    """Data structure for USD/INR exchange rate"""
    bid: float
    ask: float
    mid: float
    timestamp: datetime
    source: RateSource
    volume: Optional[float] = None
    spread: Optional[float] = None
    
    def __post_init__(self):
        """Calculate derived fields after initialization"""
        if self.mid is None:
            self.mid = (self.bid + self.ask) / 2
        if self.spread is None:
            self.spread = self.ask - self.bid
    
    def to_dict(self) -> Dict:
        """Convert rate to dictionary format"""
        return {
            'bid': self.bid,
            'ask': self.ask,
            'mid': self.mid,
            'timestamp': self.timestamp.isoformat(),
            'source': self.source.value,
            'volume': self.volume,
            'spread': self.spread
        }


class USDINRRateValidator:
    """Validator for USD/INR exchange rates"""
    
    # Reasonable bounds for USD/INR rates (as of 2024)
    MIN_RATE = 60.0
    MAX_RATE = 100.0
    MAX_SPREAD_PERCENTAGE = 2.0  # 2% maximum spread
    
    @classmethod
    def validate_rate(cls, rate: USDINRRate) -> Tuple[bool, List[str]]:
        """
        Validate a USD/INR rate for reasonableness
        
        Args:
            rate: USDINRRate instance to validate
            
        Returns:
            Tuple of (is_valid, list_of_errors)
        """
        errors = []
        
        # Check bid/ask bounds
        if rate.bid < cls.MIN_RATE or rate.bid > cls.MAX_RATE:
            errors.append(f"Bid rate {rate.bid} outside acceptable range ({cls.MIN_RATE}-{cls.MAX_RATE})")
            
        if rate.ask < cls.MIN_RATE or rate.ask > cls.MAX_RATE:
            errors.append(f"Ask rate {rate.ask} outside acceptable range ({cls.MIN_RATE}-{cls.MAX_RATE})")
        
        # Check bid <= ask
        if rate.bid > rate.ask:
            errors.append(f"Bid rate {rate.bid} cannot be higher than ask rate {rate.ask}")
        
        # Check spread reasonableness
        if rate.spread and rate.mid:
            spread_percentage = (rate.spread / rate.mid) * 100
            if spread_percentage > cls.MAX_SPREAD_PERCENTAGE:
                errors.append(f"Spread {spread_percentage:.2f}% exceeds maximum {cls.MAX_SPREAD_PERCENTAGE}%")
        
        # Check timestamp recency (not older than 24 hours)
        if rate.timestamp < datetime.now() - timedelta(hours=24):
            errors.append("Rate timestamp is older than 24 hours")
        
        return len(errors) == 0, errors


class USDINRRateCache:
    """In-memory cache for USD/INR rates with TTL support"""
    
    def __init__(self, ttl_seconds: int = 300):  # 5 minute default TTL
        self.cache: Dict[RateSource, Tuple[USDINRRate, datetime]] = {}
        self.ttl_seconds = ttl_seconds
    
    def set_rate(self, rate: USDINRRate) -> None:
        """Store rate in cache with current timestamp"""
        self.cache[rate.source] = (rate, datetime.now())
        logger.info(f"Cached USD/INR rate from {rate.source.value}: {rate.mid}")
    
    def get_rate(self, source: RateSource) -> Optional[USDINRRate]:
        """Retrieve rate from cache if not expired"""
        if source not in self.cache:
            return None
            
        rate, cached_time = self.cache[source]
        if datetime.now() - cached_time > timedelta(seconds=self.ttl_seconds):
            del self.cache[source]
            return None
            
        return rate
    
    def get_latest_rate(self) -> Optional[USDINRRate]:
        """Get the most recent cached rate from any source"""
        valid_rates = []
        for source in self.cache:
            rate = self.get_rate(source)
            if rate:
                valid_rates.append(rate)
        
        if not valid_rates:
            return None
            
        return max(valid_rates, key=lambda r: r.timestamp)
    
    def clear_expired(self) -> int:
        """Remove expired entries and return count of removed entries"""
        expired_sources = []
        for source in self.cache:
            if not self.get_rate(source):  # This will remove expired entries
                expired_sources.append(source)
        
        return len(expired_sources)


class USDINRRateProcessor:
    """Main processor for USD/INR rate operations"""
    
    def __init__(self, cache_ttl: int = 300):
        self.cache = USDINRRateCache(cache_ttl)
        self.validator = USDINRRateValidator()
        self.rate_history: List[USDINRRate] = []
    
    def process_rate(self, raw_rate_data: Dict) -> Optional[USDINRRate]:
        """
        Process raw rate data into a validated USDINRRate
        
        Args:
            raw_rate_data: Dictionary containing rate information
            
        Returns:
            USDINRRate instance if valid, None otherwise
        """
        try:
            # Extract and parse timestamp, handling timezone issues
            timestamp_str = raw_rate_data.get('timestamp', datetime.now().isoformat())
            if isinstance(timestamp_str, str):
                # Handle ISO format with Z suffix or timezone info
                if timestamp_str.endswith('Z'):
                    timestamp_str = timestamp_str[:-1] + '+00:00'
                try:
                    timestamp = datetime.fromisoformat(timestamp_str)
                    # If timezone-aware, convert to naive UTC
                    if timestamp.tzinfo is not None:
                        timestamp = timestamp.replace(tzinfo=None)
                except ValueError:
                    # Fallback to current time if parsing fails
                    timestamp = datetime.now()
            else:
                timestamp = timestamp_str if isinstance(timestamp_str, datetime) else datetime.now()
            
            # Extract data from raw input
            rate = USDINRRate(
                bid=float(raw_rate_data.get('bid', 0)),
                ask=float(raw_rate_data.get('ask', 0)),
                mid=raw_rate_data.get('mid'),
                timestamp=timestamp,
                source=RateSource(raw_rate_data.get('source', 'refinitiv')),
                volume=raw_rate_data.get('volume')
            )
            
            # Validate the rate
            is_valid, errors = self.validator.validate_rate(rate)
            if not is_valid:
                logger.error(f"Rate validation failed: {'; '.join(errors)}")
                return None
            
            # Cache the rate
            self.cache.set_rate(rate)
            
            # Add to history (keep last 1000 rates)
            self.rate_history.append(rate)
            if len(self.rate_history) > 1000:
                self.rate_history.pop(0)
            
            logger.info(f"Successfully processed USD/INR rate: {rate.mid} from {rate.source.value}")
            return rate
            
        except Exception as e:
            logger.error(f"Error processing rate data: {e}")
            return None
    
    def get_current_rate(self, preferred_source: Optional[RateSource] = None) -> Optional[USDINRRate]:
        """
        Get current USD/INR rate, optionally from a preferred source
        
        Args:
            preferred_source: Preferred rate source, fallback to any if not available
            
        Returns:
            Current USDINRRate or None if not available
        """
        if preferred_source:
            rate = self.cache.get_rate(preferred_source)
            if rate:
                return rate
        
        return self.cache.get_latest_rate()
    
    def convert_usd_to_inr(self, usd_amount: float, use_ask: bool = True) -> Optional[float]:
        """
        Convert USD amount to INR
        
        Args:
            usd_amount: Amount in USD to convert
            use_ask: If True, use ask rate (buying INR), else use bid rate
            
        Returns:
            Amount in INR or None if rate not available
        """
        rate = self.get_current_rate()
        if not rate:
            logger.warning("No current USD/INR rate available for conversion")
            return None
        
        conversion_rate = rate.ask if use_ask else rate.bid
        inr_amount = usd_amount * conversion_rate
        
        logger.info(f"Converted ${usd_amount:,.2f} USD to ₹{inr_amount:,.2f} INR at rate {conversion_rate}")
        return inr_amount
    
    def convert_inr_to_usd(self, inr_amount: float, use_bid: bool = True) -> Optional[float]:
        """
        Convert INR amount to USD
        
        Args:
            inr_amount: Amount in INR to convert
            use_bid: If True, use bid rate (selling INR), else use ask rate
            
        Returns:
            Amount in USD or None if rate not available
        """
        rate = self.get_current_rate()
        if not rate:
            logger.warning("No current USD/INR rate available for conversion")
            return None
        
        conversion_rate = rate.bid if use_bid else rate.ask
        usd_amount = inr_amount / conversion_rate
        
        logger.info(f"Converted ₹{inr_amount:,.2f} INR to ${usd_amount:,.2f} USD at rate {conversion_rate}")
        return usd_amount
    
    def get_rate_statistics(self, hours: int = 24) -> Dict:
        """
        Get rate statistics for the specified time period
        
        Args:
            hours: Number of hours to look back for statistics
            
        Returns:
            Dictionary containing rate statistics
        """
        cutoff_time = datetime.now() - timedelta(hours=hours)
        recent_rates = [r for r in self.rate_history if r.timestamp > cutoff_time]
        
        if not recent_rates:
            return {"error": f"No rates available for the last {hours} hours"}
        
        mid_rates = [r.mid for r in recent_rates]
        spreads = [r.spread for r in recent_rates if r.spread]
        
        stats = {
            "period_hours": hours,
            "rate_count": len(recent_rates),
            "mid_rate": {
                "current": recent_rates[-1].mid,
                "high": max(mid_rates),
                "low": min(mid_rates),
                "average": sum(mid_rates) / len(mid_rates)
            },
            "volatility": {
                "range": max(mid_rates) - min(mid_rates),
                "range_percentage": ((max(mid_rates) - min(mid_rates)) / (sum(mid_rates) / len(mid_rates))) * 100
            },
            "sources": list(set(r.source.value for r in recent_rates))
        }
        
        if spreads:
            stats["spread"] = {
                "current": recent_rates[-1].spread,
                "average": sum(spreads) / len(spreads),
                "max": max(spreads),
                "min": min(spreads)
            }
        
        return stats
    
    def export_rates(self, format_type: str = "json") -> str:
        """
        Export rate history in specified format
        
        Args:
            format_type: Export format ('json' or 'csv')
            
        Returns:
            Formatted string of rate data
        """
        if format_type.lower() == "json":
            return json.dumps([rate.to_dict() for rate in self.rate_history], indent=2)
        elif format_type.lower() == "csv":
            if not self.rate_history:
                return "No data available"
            
            header = "timestamp,source,bid,ask,mid,spread,volume\n"
            rows = []
            for rate in self.rate_history:
                rows.append(f"{rate.timestamp.isoformat()},{rate.source.value},{rate.bid},{rate.ask},{rate.mid},{rate.spread},{rate.volume or ''}")
            
            return header + "\n".join(rows)
        else:
            raise ValueError(f"Unsupported format: {format_type}")


# Global instance for easy access
usd_inr_processor = USDINRRateProcessor()


def process_usd_inr_rate(rate_data: Dict) -> Optional[USDINRRate]:
    """Convenience function to process a USD/INR rate"""
    return usd_inr_processor.process_rate(rate_data)


def get_usd_inr_rate(source: Optional[str] = None) -> Optional[USDINRRate]:
    """Convenience function to get current USD/INR rate"""
    source_enum = RateSource(source) if source else None
    return usd_inr_processor.get_current_rate(source_enum)


def convert_usd_to_inr(amount: float) -> Optional[float]:
    """Convenience function for USD to INR conversion"""
    return usd_inr_processor.convert_usd_to_inr(amount)


def convert_inr_to_usd(amount: float) -> Optional[float]:
    """Convenience function for INR to USD conversion"""
    return usd_inr_processor.convert_inr_to_usd(amount)


if __name__ == "__main__":
    # Example usage
    print("USD/INR Rate Logic Module")
    print("=" * 50)
    
    # Sample rate data
    sample_rate = {
        "bid": 83.25,
        "ask": 83.30,
        "timestamp": datetime.now().isoformat(),
        "source": "refinitiv",
        "volume": 1000000
    }
    
    # Process sample rate
    processed_rate = process_usd_inr_rate(sample_rate)
    if processed_rate:
        print(f"Processed rate: {processed_rate}")
        
        # Test conversions
        usd_amount = 1000
        inr_result = convert_usd_to_inr(usd_amount)
        if inr_result:
            print(f"${usd_amount} USD = ₹{inr_result:,.2f} INR")
        
        inr_amount = 100000
        usd_result = convert_inr_to_usd(inr_amount)
        if usd_result:
            print(f"₹{inr_amount} INR = ${usd_result:,.2f} USD")
        
        # Get statistics
        stats = usd_inr_processor.get_rate_statistics(1)
        print(f"Rate statistics: {stats}")
    else:
        print("Failed to process sample rate")