package ac.bbau.library.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Asynchronous HTTP client for the existing Spring Boot REST API. */
final class ApiClient {
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12)).build();
    private final String baseUrl;

    ApiClient() {
        String configured = System.getProperty("library.api.url");
        if (configured == null || configured.isBlank()) configured = System.getenv("LIBRARY_API_URL");
        if (configured == null || configured.isBlank()) configured = "http://localhost:8080";
        baseUrl = configured.replaceAll("/+$", "");
    }

    String baseUrl() { return baseUrl; }

    CompletableFuture<JsonNode> get(String path, String token) {
        return send("GET", path, null, token);
    }

    CompletableFuture<JsonNode> post(String path, Object data, String token) {
        return send("POST", path, data, token);
    }

    CompletableFuture<JsonNode> put(String path, Object data, String token) {
        return send("PUT", path, data, token);
    }

    CompletableFuture<JsonNode> delete(String path, String token) {
        return send("DELETE", path, null, token);
    }

    <T> List<T> list(JsonNode data, TypeReference<List<T>> type) {
        return json.convertValue(data, type);
    }

    JsonNode parse(String body) {
        try { return json.readTree(body); }
        catch (Exception exception) { return json.getNodeFactory().nullNode(); }
    }

    String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    private CompletableFuture<JsonNode> send(String method, String path, Object data, String token) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(25)).header("Accept", "application/json");
        if (token != null && !token.isBlank()) request.header("Authorization", "Bearer " + token);
        try {
            if (data == null) request.method(method, HttpRequest.BodyPublishers.noBody());
            else request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data)));
        } catch (Exception exception) {
            return CompletableFuture.failedFuture(new ApiException("Could not prepare the request."));
        }
        return http.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    JsonNode result = parse(response.body());
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        String message = result.path("message").asText();
                        if (message.isBlank()) message = result.path("error").asText();
                        if (message.isBlank()) message = "Request failed (HTTP " + response.statusCode() + ").";
                        throw new ApiException(message);
                    }
                    return result;
                });
    }

    static Map<String, Object> data(Object... entries) {
        var map = new java.util.LinkedHashMap<String, Object>();
        for (int i = 0; i < entries.length; i += 2) map.put((String) entries[i], entries[i + 1]);
        return map;
    }

    static final class ApiException extends RuntimeException {
        ApiException(String message) { super(message); }
    }
}
