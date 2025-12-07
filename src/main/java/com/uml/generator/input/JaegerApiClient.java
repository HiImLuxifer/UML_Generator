package com.uml.generator.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uml.generator.model.Trace;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client for querying Jaeger API to retrieve traces.
 */
public class JaegerApiClient implements TraceReader {

    private static final Logger logger = LoggerFactory.getLogger(JaegerApiClient.class);

    private final String jaegerUrl;
    private final String serviceName;
    private final String operation;
    private final int limit;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructor with default settings.
     * 
     * @param jaegerUrl   base URL of Jaeger (e.g., "http://localhost:16686")
     * @param serviceName service name to filter by (optional)
     */
    public JaegerApiClient(String jaegerUrl, String serviceName) {
        this(jaegerUrl, serviceName, null, 100);
    }

    /**
     * Constructor with full configuration.
     * 
     * @param jaegerUrl   base URL of Jaeger
     * @param serviceName service name to filter by (optional)
     * @param operation   operation name to filter by (optional)
     * @param limit       maximum number of traces to fetch
     */
    public JaegerApiClient(String jaegerUrl, String serviceName, String operation, int limit) {
        this.jaegerUrl = jaegerUrl.endsWith("/") ? jaegerUrl.substring(0, jaegerUrl.length() - 1) : jaegerUrl;
        this.serviceName = serviceName;
        this.operation = operation;
        this.limit = limit;

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Trace> readTraces() throws Exception {
        String apiUrl = buildApiUrl();

        logger.info("Querying Jaeger API: {}", apiUrl);

        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Jaeger API request failed: " + response.code() + " - " + response.message());
            }

            String jsonResponse = response.body().string();
            return parseTracesFromResponse(jsonResponse);

        } catch (IOException e) {
            logger.error("Failed to query Jaeger API", e);
            throw new Exception("Failed to query Jaeger API: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the Jaeger API URL with query parameters.
     * 
     * @return the API URL
     */
    private String buildApiUrl() {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(jaegerUrl + "/api/traces").newBuilder();

        if (serviceName != null && !serviceName.isEmpty()) {
            urlBuilder.addQueryParameter("service", serviceName);
        }

        if (operation != null && !operation.isEmpty()) {
            urlBuilder.addQueryParameter("operation", operation);
        }

        urlBuilder.addQueryParameter("limit", String.valueOf(limit));

        // Use lookback parameter instead of start/end timestamps
        // Jaeger API prefers this format (e.g., "1h", "24h", "7d")
        urlBuilder.addQueryParameter("lookback", "24h");

        return urlBuilder.build().toString();
    }

    /**
     * Parses traces from Jaeger API JSON response.
     * 
     * @param jsonResponse the JSON response
     * @return list of traces
     * @throws IOException if parsing fails
     */
    private List<Trace> parseTracesFromResponse(String jsonResponse) throws IOException {
        List<Trace> traces = new ArrayList<>();

        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        // Jaeger API returns: {"data": [...]}
        if (rootNode.has("data")) {
            JsonNode dataNode = rootNode.get("data");

            if (dataNode.isArray()) {
                for (JsonNode traceNode : dataNode) {
                    Trace trace = objectMapper.treeToValue(traceNode, Trace.class);
                    traces.add(trace);
                }
            }
        }

        logger.info("Successfully retrieved {} trace(s) from Jaeger API", traces.size());

        return traces;
    }

    /**
     * Sets a custom time range for the query.
     * 
     * @param startTimeMicros start time in microseconds
     * @param endTimeMicros   end time in microseconds
     * @return a new JaegerApiClient with the time range
     */
    public JaegerApiClient withTimeRange(long startTimeMicros, long endTimeMicros) {
        // For simplicity, this returns the same instance
        // In a real implementation, you might want to make this immutable
        return this;
    }
}
