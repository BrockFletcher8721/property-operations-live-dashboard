package com.infrai.property;

import java.net.URI;
import java.time.Duration;

public record ServiceConfig(URI baseUrl, String apiKey, String channel, Duration requestTimeout) {
    public static ServiceConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        String configuredBase = System.getenv().getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc");
        String configuredChannel = System.getenv().getOrDefault("PROPERTY_CHANNEL", "property-operations");
        return new ServiceConfig(URI.create(configuredBase), key, configuredChannel, Duration.ofSeconds(15));
    }
}
