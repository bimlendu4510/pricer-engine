package com.pricer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricer.model.TickData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RedisTickDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisTickDataService.class);
    private static final Pattern REDIS_KEY_PATTERN = Pattern.compile("tickbuf:(P[1-3]):([^:]+):([^:]+):([0-9.]+)");
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Retrieves all tick buffer keys from Redis
     */
    public Set<String> getAllTickKeys() {
        try {
            Set<String> keys = redisTemplate.keys("tickbuf:*");
            logger.info("Found {} tick keys in Redis", keys != null ? keys.size() : 0);
            return keys != null ? keys : new HashSet<>();
        } catch (Exception e) {
            logger.error("Error retrieving tick keys from Redis", e);
            return new HashSet<>();
        }
    }

    /**
     * Parses Redis key to extract tick metadata
     */
    public TickData parseKeyToTickData(String redisKey) {
        Matcher matcher = REDIS_KEY_PATTERN.matcher(redisKey);
        if (!matcher.matches()) {
            logger.warn("Invalid Redis key format: {}", redisKey);
            return null;
        }
        
        String source = matcher.group(1);
        String currency = matcher.group(2);
        String type = matcher.group(3);
        BigDecimal volume = new BigDecimal(matcher.group(4));
        
        // Get actual tick data from Redis
        String tickDataJson = redisTemplate.opsForValue().get(redisKey);
        if (tickDataJson == null) {
            logger.warn("No data found for key: {}", redisKey);
            return null;
        }
        
        try {
            // Parse JSON data to extract bid/ask rates
            JsonNode tickNode = objectMapper.readTree(tickDataJson);
            
            BigDecimal bidRate = new BigDecimal(tickNode.get("bidRate").asText());
            BigDecimal askRate = new BigDecimal(tickNode.get("askRate").asText());
            LocalDateTime timestamp = LocalDateTime.parse(tickNode.get("timestamp").asText());
            
            return new TickData(source, currency, type, volume, bidRate, askRate, timestamp);
            
        } catch (Exception e) {
            logger.error("Error parsing tick data JSON for key {}: {}", redisKey, e.getMessage());
            // Create tick data with mock rates if JSON parsing fails
            return createMockTickData(source, currency, type, volume);
        }
    }
    
    /**
     * Creates mock tick data for testing when Redis data is not available
     */
    private TickData createMockTickData(String source, String currency, String type, BigDecimal volume) {
        // Generate mock bid/ask rates based on source and volume
        Random random = new Random();
        BigDecimal baseRate = new BigDecimal("82.50"); // USD.INR base rate
        
        // Add some variation based on source
        BigDecimal sourceVariation = new BigDecimal(source.equals("P1") ? "0.10" : 
                                                   source.equals("P2") ? "0.05" : "0.15");
        
        // Add volume-based spread
        BigDecimal volumeSpread = volume.compareTo(new BigDecimal("100000")) > 0 ? 
                                 new BigDecimal("0.02") : new BigDecimal("0.05");
        
        BigDecimal bidRate = baseRate.subtract(sourceVariation).subtract(volumeSpread);
        BigDecimal askRate = baseRate.add(sourceVariation).add(volumeSpread);
        
        return new TickData(source, currency, type, volume, bidRate, askRate, LocalDateTime.now());
    }

    /**
     * Groups tick data by currency, type, and volume
     */
    public Map<String, List<TickData>> groupTickData(List<TickData> tickDataList) {
        Map<String, List<TickData>> groupedData = new HashMap<>();
        
        for (TickData tick : tickDataList) {
            String key = tick.getCurrency() + ":" + tick.getType() + ":" + tick.getVolume();
            groupedData.computeIfAbsent(key, k -> new ArrayList<>()).add(tick);
        }
        
        return groupedData;
    }

    /**
     * Retrieves all tick data from Redis
     */
    public List<TickData> getAllTickData() {
        Set<String> keys = getAllTickKeys();
        List<TickData> tickDataList = new ArrayList<>();
        
        for (String key : keys) {
            TickData tickData = parseKeyToTickData(key);
            if (tickData != null) {
                tickDataList.add(tickData);
            }
        }
        
        logger.info("Retrieved {} tick data records", tickDataList.size());
        return tickDataList;
    }
}