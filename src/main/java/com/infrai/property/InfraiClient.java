package com.infrai.property;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class InfraiClient {
    private static final int MAX_ATTEMPTS = 4;
    private final ServiceConfig config;
    private final HttpClient http;

    public InfraiClient(ServiceConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder().connectTimeout(config.requestTimeout()).build();
    }

    public Object queryMetrics() throws IOException, InterruptedException {
        return request("GET", "/v1/metrics/query?name=property.operations&agg=sum", null, null);
    }

    public Object createChannel() throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channel", config.channel());
        body.put("type", "public");
        body.put("vendor", "infrai");
        return request("POST", "/v1/realtime/channel/create", body, idempotency("channel"));
    }

    public Object publish(Object metrics, PropertyPortfolio.DashboardSnapshot snapshot)
            throws IOException, InterruptedException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("metrics", metrics);
        data.put("propertyOperations", snapshot.asMap());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channel", config.channel());
        body.put("event", "property.operations.updated");
        body.put("data", data);
        body.put("account_id", "property-demo");
        return request("POST", "/v1/realtime/publish", body, idempotency("publish"));
    }

    private String idempotency(String operation) {
        return UUID.nameUUIDFromBytes((operation + ":" + config.channel()).getBytes()).toString();
    }

    @SuppressWarnings("unchecked")
    private Object request(String method, String path, Map<String, Object> body, String idempotencyKey)
            throws IOException, InterruptedException {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + path))
                    .timeout(config.requestTimeout())
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Accept", "application/json");
            if (idempotencyKey != null) builder.header("Idempotency-Key", idempotencyKey);
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(Json.stringify(body));
            if (body != null) builder.header("Content-Type", "application/json");
            builder.method(method, publisher);

            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Object decoded;
            try { decoded = Json.parse(response.body()); }
            catch (IllegalArgumentException malformed) {
                if (response.statusCode() >= 500) throw new IOException("Infrai transport response " + response.statusCode(), malformed);
                throw new IOException("Expected an Infrai response envelope", malformed);
            }
            if (!(decoded instanceof Map<?, ?> rawEnvelope)) throw new IOException("Expected an Infrai response envelope");
            Map<String, Object> envelope = (Map<String, Object>) rawEnvelope;

            if (response.statusCode() == 429 && attempt + 1 < MAX_ATTEMPTS) {
                Thread.sleep(retryDelay(response, attempt).toMillis());
                continue;
            }
            if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                Map<String, Object> error = envelope.get("error") instanceof Map<?, ?> value
                        ? (Map<String, Object>) value : Map.of("message", "Request rejected");
                throw new InfraiException(String.valueOf(error.getOrDefault("code", "REQUEST_REJECTED")), error, response.statusCode());
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai transport response " + response.statusCode());
            return envelope.get("data");
        }
        throw new IOException("Retry budget exhausted");
    }

    private Duration retryDelay(HttpResponse<?> response, int attempt) {
        String value = response.headers().firstValue("Retry-After").orElse("").trim();
        try { return Duration.ofSeconds(Math.max(1, Long.parseLong(value))); }
        catch (NumberFormatException ignored) { return Duration.ofMillis(250L * (1L << attempt)); }
    }

    public static final class InfraiException extends IOException {
        private static final long serialVersionUID = 1L;
        private final String code;
        private final Map<String, Object> details;
        private final int statusCode;

        InfraiException(String code, Map<String, Object> details, int statusCode) {
            super(String.valueOf(details.getOrDefault("message", code)));
            this.code = code;
            this.details = Map.copyOf(details);
            this.statusCode = statusCode;
        }
        public String code() { return code; }
        public Map<String, Object> details() { return details; }
        public int statusCode() { return statusCode; }
    }
}
