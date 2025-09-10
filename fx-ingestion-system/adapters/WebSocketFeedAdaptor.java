package adapters;

public class WebSocketFeedAdaptor implements MarketFeedAdaptor {
    public void initialize() {
        // Connect to WebSocket and publish messages to Kafka
    }

    public String sourceName() {
        return "WebSocket";
    }
}
