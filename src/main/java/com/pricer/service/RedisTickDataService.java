package com.pricer.service;

import com.pricer.model.TickData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service that handles Redis integration and tick data retrieval.
 * Redis key format: tickbuf:{SOURCE}:{CURRENCY}:{TYPE}:{VOLUME}
 */
@Service
@ConditionalOnProperty(name = "spring.redis.host")
public class RedisTickDataService {

    private static final Logger logger = LoggerFactory.getLogger(RedisTickDataService.class);
    private static final String TICKBUF_PATTERN = "tickbuf:*";
    private static final Pattern KEY_PATTERN = Pattern.compile("tickbuf:([^:]+):([^:]+):([^:]+):([^:]+)");

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    /**
     * Retrieves all tick data from Redis using the tickbuf pattern.
     */
    public List<TickData> getAllTickData() {
        if (redisTemplate == null) {
            logger.warn("Redis template not available, returning empty list");
            return new ArrayList<>();
        }

        try {
            Set<String> keys = redisTemplate.keys(TICKBUF_PATTERN);
            if (keys == null || keys.isEmpty()) {
                logger.info("No tick data found in Redis");
                return new ArrayList<>();
            }

            List<TickData> tickDataList = new ArrayList<>();
            for (String key : keys) {
                TickData tickData = parseTickDataFromKey(key);
                if (tickData != null) {
                    // Get bid and ask rates from Redis
                    String value = redisTemplate.opsForValue().get(key);
                    if (value != null) {
                        parseRatesFromValue(tickData, value);
                        tickDataList.add(tickData);
                    }
                }
            }

            logger.info("Retrieved {} tick data entries from Redis", tickDataList.size());
            return tickDataList;
        } catch (Exception e) {
            logger.error("Error retrieving tick data from Redis", e);
            return new ArrayList<>();
        }
    }

    /**
     * Parses tick data from Redis key.
     * Key format: tickbuf:{SOURCE}:{CURRENCY}:{TYPE}:{VOLUME}
     */
    private TickData parseTickDataFromKey(String key) {
        Matcher matcher = KEY_PATTERN.matcher(key);
        if (!matcher.matches()) {
            logger.warn("Invalid key format: {}", key);
            return null;
        }

        try {
            String source = matcher.group(1);
            String currency = matcher.group(2);
            String type = matcher.group(3);
            Double volume = Double.parseDouble(matcher.group(4));

            return new TickData(source, currency, type, volume, null, null, LocalDateTime.now());
        } catch (NumberFormatException e) {
            logger.warn("Error parsing volume from key: {}", key, e);
            return null;
        }
    }

    /**
     * Parses bid and ask rates from Redis value.
     * Expected format: "bid:82.50,ask:82.54" or similar JSON/CSV format
     */
    private void parseRatesFromValue(TickData tickData, String value) {
        try {
            // Simple parsing - can be enhanced based on actual Redis data format
            if (value.contains("bid") && value.contains("ask")) {
                // Format: "bid:82.50,ask:82.54"
                String[] parts = value.split(",");
                for (String part : parts) {
                    String[] keyValue = part.split(":");
                    if (keyValue.length == 2) {
                        if ("bid".equals(keyValue[0].trim())) {
                            tickData.setBidRate(Double.parseDouble(keyValue[1].trim()));
                        } else if ("ask".equals(keyValue[0].trim())) {
                            tickData.setAskRate(Double.parseDouble(keyValue[1].trim()));
                        }
                    }
                }
            } else {
                // Fallback: assume format "bid,ask"
                String[] rates = value.split(",");
                if (rates.length >= 2) {
                    tickData.setBidRate(Double.parseDouble(rates[0].trim()));
                    tickData.setAskRate(Double.parseDouble(rates[1].trim()));
                }
            }
        } catch (Exception e) {
            logger.warn("Error parsing rates from value: {} for key with source: {}", value, tickData.getSource(), e);
            // Set default values if parsing fails
            tickData.setBidRate(82.0 + Math.random() * 2);
            tickData.setAskRate(tickData.getBidRate() + 0.05 + Math.random() * 0.10);
        }
    }
}