package adapters;

/**
 * Interface for market feed adaptors that ingest FX rate data from various sources
 */
public interface MarketFeedAdaptor {
    
    /**
     * Initialize the adaptor and start data ingestion
     */
    void initialize();
    
    /**
     * Get the source name for this adaptor
     * @return source name (e.g., "REFINITIV", "FXCLEAR", "JPMORGAN")
     */
    String sourceName();
    
    /**
     * Stop the adaptor and clean up resources
     */
    default void shutdown() {
        // Default implementation - do nothing
    }
}