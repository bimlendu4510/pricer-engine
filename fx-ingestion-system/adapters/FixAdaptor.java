package adapters;

public class FixAdaptor implements MarketFeedAdaptor {
    public void initialize() {
        // Use QuickFIX/J to connect and ingest FX data
    }

    public String sourceName() {
        return "FIX";
    }
}
