package com.uml.generator.analyzer;

import com.uml.generator.model.Span;
import com.uml.generator.model.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Aggregates and analyzes data from multiple traces to build
 * a comprehensive view of the system architecture.
 */
public class TraceAggregator {

    private static final Logger logger = LoggerFactory.getLogger(TraceAggregator.class);

    private final List<Trace> traces;
    private Set<String> allServices;
    private Map<String, Set<String>> serviceOperations;
    private Map<String, Set<String>> serviceDependencies;
    private Map<String, Map<String, Object>> serviceMetadata;
    // Map: fromService -> toService -> Set of operations called
    private Map<String, Map<String, Set<String>>> serviceCalls;

    public TraceAggregator(List<Trace> traces) {
        this.traces = traces != null ? traces : new ArrayList<>();
        analyze();
    }

    /**
     * Analyzes all traces to extract aggregated information.
     */
    private void analyze() {
        logger.info("Analyzing {} trace(s)", traces.size());

        allServices = new HashSet<>();
        serviceOperations = new HashMap<>();
        serviceDependencies = new HashMap<>();
        serviceMetadata = new HashMap<>();
        serviceCalls = new HashMap<>();

        for (Trace trace : traces) {
            analyzeTrace(trace);
        }

        logger.info("Found {} unique service(s)", allServices.size());
        logger.info("Service list: {}", allServices);
    }

    /**
     * Analyzes a single trace.
     */
    private void analyzeTrace(Trace trace) {
        if (trace == null || trace.getSpans() == null) {
            return;
        }

        for (Span span : trace.getSpans()) {
            String serviceName = trace.getServiceName(span);

            // Collect service
            allServices.add(serviceName);

            // Collect operations
            serviceOperations.computeIfAbsent(serviceName, k -> new HashSet<>())
                    .add(span.getOperationName());

            // Collect metadata from process tags
            if (span.getProcessID() != null && trace.getProcess(span.getProcessID()) != null) {
                List<Map<String, Object>> tags = trace.getProcess(span.getProcessID()).getTags();
                if (tags != null) {
                    Map<String, Object> metadata = serviceMetadata.computeIfAbsent(serviceName, k -> new HashMap<>());
                    // Convert tag array to map for easier access
                    for (Map<String, Object> tag : tags) {
                        Object key = tag.get("key");
                        Object value = tag.get("value");
                        if (key != null && value != null) {
                            metadata.put(key.toString(), value);
                        }
                    }
                }
            }

            // Analyze dependencies (parent-child relationships)
            String parentSpanId = span.getParentSpanId();
            if (parentSpanId != null) {
                Span parentSpan = trace.getSpan(parentSpanId);
                if (parentSpan != null) {
                    String parentService = trace.getServiceName(parentSpan);
                    if (!serviceName.equals(parentService)) {
                        // Cross-service dependency
                        serviceDependencies.computeIfAbsent(parentService, k -> new HashSet<>())
                                .add(serviceName);

                        // Track specific operation calls between services
                        serviceCalls.computeIfAbsent(parentService, k -> new HashMap<>())
                                .computeIfAbsent(serviceName, k -> new HashSet<>())
                                .add(span.getOperationName());
                    }
                }
            }
        }
    }

    public Set<String> getAllServices() {
        return new HashSet<>(allServices);
    }

    public Map<String, Set<String>> getServiceOperations() {
        return new HashMap<>(serviceOperations);
    }

    public Map<String, Set<String>> getServiceDependencies() {
        return new HashMap<>(serviceDependencies);
    }

    public Map<String, Map<String, Object>> getServiceMetadata() {
        return new HashMap<>(serviceMetadata);
    }

    /**
     * Gets all operations for a specific service.
     */
    public Set<String> getOperationsForService(String serviceName) {
        return serviceOperations.getOrDefault(serviceName, new HashSet<>());
    }

    /**
     * Gets all services that depend on a specific service.
     */
    public Set<String> getDependenciesForService(String serviceName) {
        return serviceDependencies.getOrDefault(serviceName, new HashSet<>());
    }

    /**
     * Gets metadata for a specific service.
     */
    public Map<String, Object> getMetadataForService(String serviceName) {
        return serviceMetadata.getOrDefault(serviceName, new HashMap<>());
    }

    public List<Trace> getTraces() {
        return new ArrayList<>(traces);
    }

    /**
     * Gets all service calls with specific operations.
     * Returns a map: fromService -> toService -> Set of operations called
     */
    public Map<String, Map<String, Set<String>>> getServiceCalls() {
        return new HashMap<>(serviceCalls);
    }
}
