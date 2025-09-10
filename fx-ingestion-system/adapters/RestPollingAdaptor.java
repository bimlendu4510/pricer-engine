package adapters;

public class RestPollingAdaptor implements MarketFeedAdaptor {
    public void initialize() {
        // Poll REST endpoint and publish messages to Kafka
    }

    public String sourceName() {
        return "REST";
    }
}
