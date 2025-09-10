package com.pricer.config;

import com.pricer.model.PricingStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class that manages source-to-strategy mappings.
 */
@Configuration
@ConfigurationProperties(prefix = "pricing")
public class PricingConfig {

    private Map<String, PricingStrategy> sourceStrategies = new HashMap<>();

    public PricingConfig() {
        // Default configuration as specified in the problem statement
        sourceStrategies.put("P1", PricingStrategy.BEST_PRICE);
        sourceStrategies.put("P2", PricingStrategy.DEEP_AVG);
        sourceStrategies.put("P3", PricingStrategy.VWAP);
    }

    public Map<String, PricingStrategy> getSourceStrategies() {
        return sourceStrategies;
    }

    public void setSourceStrategies(Map<String, PricingStrategy> sourceStrategies) {
        this.sourceStrategies = sourceStrategies;
    }

    public PricingStrategy getStrategyForSource(String source) {
        return sourceStrategies.get(source);
    }

    public void setStrategyForSource(String source, PricingStrategy strategy) {
        sourceStrategies.put(source, strategy);
    }
}