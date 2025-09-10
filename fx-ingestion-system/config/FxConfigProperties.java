package config;

import java.util.Map;

public class FxConfigProperties {
    private Map<String, FxSourceConfig> sources;

    public Map<String, FxSourceConfig> getSources() {
        return sources;
    }

    public void setSources(Map<String, FxSourceConfig> sources) {
        this.sources = sources;
    }
}
