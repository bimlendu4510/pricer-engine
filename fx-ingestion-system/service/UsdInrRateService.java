package service;

import model.StandardMarketRate;
import model.UsdInrRateProcessor;
import kafka.FxKafkaProducer;
import java.time.LocalDateTime;

/**
 * Service class that integrates USD-INR rate processing with the FX ingestion system
 * This class can be called by various adaptors when they receive USD-INR rate updates
 */
public class UsdInrRateService {
    
    private final UsdInrRateProcessor processor;
    private final FxKafkaProducer kafkaProducer;
    
    public UsdInrRateService() {
        this.processor = new UsdInrRateProcessor();
        this.kafkaProducer = new FxKafkaProducer();
    }
    
    public UsdInrRateService(UsdInrRateProcessor processor, FxKafkaProducer kafkaProducer) {
        this.processor = processor;
        this.kafkaProducer = kafkaProducer;
    }
    
    /**
     * Process incoming USD-INR rate and publish the processed rate to Kafka
     * 
     * @param incomingRate Raw rate data from market feed
     * @return Processed rate that was published
     */
    public StandardMarketRate processAndPublishRate(StandardMarketRate incomingRate) {
        try {
            // Validate input
            if (incomingRate == null) {
                throw new IllegalArgumentException("Incoming rate cannot be null");
            }
            
            if (!"USD-INR".equals(incomingRate.getSymbol())) {
                throw new IllegalArgumentException("This service only processes USD-INR rates");
            }
            
            // Process the rate using business logic
            StandardMarketRate processedRate = processor.processUsdInrRate(incomingRate);
            
            // Add processing timestamp
            processedRate.setTimestamp(LocalDateTime.now());
            
            // Publish to Kafka for downstream consumption
            kafkaProducer.sendToKafka(processedRate);
            
            return processedRate;
            
        } catch (Exception e) {
            System.err.println("Error processing USD-INR rate: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Process rate without publishing (for testing or validation)
     */
    public StandardMarketRate processRateOnly(StandardMarketRate incomingRate) {
        return processor.processUsdInrRate(incomingRate);
    }
}