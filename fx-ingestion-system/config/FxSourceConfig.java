package config;

import java.util.Map;

public class FxSourceConfig {
    private String type;
    private boolean enabled;
    private String url;
    private String baseUrl;
    private Long pollingIntervalMs;
    private Map<String, String> connection;
    
    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public Long getPollingIntervalMs() { return pollingIntervalMs; }
    public void setPollingIntervalMs(Long pollingIntervalMs) { this.pollingIntervalMs = pollingIntervalMs; }
    
    public Map<String, String> getConnection() { return connection; }
    public void setConnection(Map<String, String> connection) { this.connection = connection; }
}