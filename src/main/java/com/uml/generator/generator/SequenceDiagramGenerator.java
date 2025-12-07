package com.uml.generator.generator;

import com.uml.generator.model.Span;
import com.uml.generator.model.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates UML Sequence Diagrams from Jaeger traces.
 * Each trace produces one sequence diagram showing the flow of calls.
 */
public class SequenceDiagramGenerator implements DiagramGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SequenceDiagramGenerator.class);

    @Override
    public String getDiagramType() {
        return "sequence";
    }

    @Override
    public String generatePlantUML(List<Trace> traces) {
        if (traces == null || traces.isEmpty()) {
            logger.warn("No traces provided for sequence diagram generation");
            return "";
        }

        // For sequence diagrams, we generate one diagram per trace
        // If multiple traces, we'll combine them or you can modify to generate separate
        // files
        StringBuilder allDiagrams = new StringBuilder();

        for (int i = 0; i < traces.size(); i++) {
            Trace trace = traces.get(i);
            String diagram = generateSequenceDiagramForTrace(trace, i);
            allDiagrams.append(diagram);

            if (i < traces.size() - 1) {
                allDiagrams.append("\n\n");
            }
        }

        return allDiagrams.toString();
    }

    /**
     * Generates a sequence diagram for a single trace with deduplication.
     */
    private String generateSequenceDiagramForTrace(Trace trace, int index) {
        StringBuilder puml = new StringBuilder();

        puml.append("@startuml\n");
        puml.append("title Sequence Diagram - Trace ").append(index + 1).append("\n");
        puml.append("autonumber\n\n");

        // Get all participants (services)
        List<String> services = trace.getAllServiceNames();
        for (String service : services) {
            puml.append("participant \"").append(service).append("\" as ").append(sanitizeId(service)).append("\n");
        }
        puml.append("\n");

        // Process spans in chronological order
        List<Span> sortedSpans = trace.getSpansSortedByTime();
        Map<String, String> spanToService = new HashMap<>();

        for (Span span : sortedSpans) {
            spanToService.put(span.getSpanID(), trace.getServiceName(span));
        }

        // Track last call for deduplication
        String lastCall = null;
        int callCount = 0;

        // Generate interactions with deduplication
        for (Span span : sortedSpans) {
            String currentService = trace.getServiceName(span);
            String parentSpanId = span.getParentSpanId();

            if (parentSpanId != null) {
                String parentService = spanToService.get(parentSpanId);

                if (parentService != null && !parentService.equals(currentService)) {
                    // Cross-service call
                    String cleanOperation = com.uml.generator.util.NameUtils
                            .cleanOperationName(span.getOperationName());
                    String currentCall = parentService + "->" + currentService + ":" + cleanOperation;

                    // Check for deduplication
                    if (currentCall.equals(lastCall)) {
                        callCount++;
                    } else {
                        // Output previous call with count if needed
                        if (lastCall != null && callCount > 0) {
                            outputCallWithCount(puml, lastCall, callCount);
                        }

                        // Reset for new call
                        lastCall = currentCall;
                        callCount = 1;
                    }
                }
            } else if (span.isRootSpan()) {
                // Flush any pending deduplicated call first
                if (lastCall != null && callCount > 0) {
                    outputCallWithCount(puml, lastCall, callCount);
                    lastCall = null;
                    callCount = 0;
                }

                // Root span - external trigger or user
                String cleanOperation = com.uml.generator.util.NameUtils.cleanOperationName(span.getOperationName());
                puml.append("[-> ")
                        .append(sanitizeId(currentService))
                        .append(": ")
                        .append(cleanOperation)
                        .append("\n");
            }
        }

        // Output final deduplicated call if exists
        if (lastCall != null && callCount > 0) {
            outputCallWithCount(puml, lastCall, callCount);
        }

        puml.append("\n@enduml");

        logger.info("Generated sequence diagram for trace {}", trace.getTraceID());

        return puml.toString();
    }

    /**
     * Helper method to output a call with optional multiplicity notation.
     */
    private void outputCallWithCount(StringBuilder puml, String callSignature, int count) {
        // Parse call signature: "fromService->toService:operation"
        String[] parts = callSignature.split("->");
        if (parts.length != 2)
            return;

        String fromService = parts[0];
        String[] toParts = parts[1].split(":", 2);
        if (toParts.length != 2)
            return;

        String toService = toParts[0];
        String operation = toParts[1];

        // Output call
        puml.append(sanitizeId(fromService))
                .append(" -> ")
                .append(sanitizeId(toService))
                .append(": ")
                .append(operation);

        if (count > 1) {
            puml.append(" [x").append(count).append("]");
        }

        puml.append("\n");

        // Return message
        puml.append(sanitizeId(toService))
                .append(" --> ")
                .append(sanitizeId(fromService))
                .append("\n");
    }

    /**
     * Sanitizes a service name to be used as a PlantUML ID.
     */
    private String sanitizeId(String name) {
        if (name == null)
            return "unknown";
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
