package com.uml.generator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a complete Jaeger trace.
 * A trace contains multiple spans and process information.
 */
public class Trace {

    @JsonProperty("traceID")
    private String traceID;

    @JsonProperty("spans")
    private List<Span> spans;

    @JsonProperty("processes")
    private Map<String, Process> processes;

    @JsonProperty("warnings")
    private List<String> warnings;

    // Non-JSON field: tracks the source filename for this trace
    private String sourceName;

    public Trace() {
        this.spans = new ArrayList<>();
        this.processes = new HashMap<>();
    }

    public String getTraceID() {
        return traceID;
    }

    public void setTraceID(String traceID) {
        this.traceID = traceID;
    }

    public List<Span> getSpans() {
        return spans;
    }

    public void setSpans(List<Span> spans) {
        this.spans = spans;
    }

    public Map<String, Process> getProcesses() {
        return processes;
    }

    public void setProcesses(Map<String, Process> processes) {
        this.processes = processes;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * Gets a process by its ID.
     * 
     * @param processID the process ID
     * @return the Process or null if not found
     */
    public Process getProcess(String processID) {
        return processes != null ? processes.get(processID) : null;
    }

    /**
     * Gets the service name for a given span.
     * 
     * @param span the span
     * @return the service name or "unknown"
     */
    public String getServiceName(Span span) {
        if (span == null || span.getProcessID() == null) {
            return "unknown";
        }

        Process process = getProcess(span.getProcessID());
        return process != null ? process.getServiceName() : "unknown";
    }

    /**
     * Gets a span by its ID.
     * 
     * @param spanID the span ID
     * @return the Span or null if not found
     */
    public Span getSpan(String spanID) {
        if (spans == null)
            return null;

        return spans.stream()
                .filter(s -> spanID.equals(s.getSpanID()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets all root spans (spans with no parent).
     * 
     * @return list of root spans
     */
    public List<Span> getRootSpans() {
        if (spans == null)
            return new ArrayList<>();

        return spans.stream()
                .filter(Span::isRootSpan)
                .collect(Collectors.toList());
    }

    /**
     * Gets child spans of a given parent span.
     * 
     * @param parentSpanId the parent span ID
     * @return list of child spans
     */
    public List<Span> getChildSpans(String parentSpanId) {
        if (spans == null)
            return new ArrayList<>();

        return spans.stream()
                .filter(s -> parentSpanId.equals(s.getParentSpanId()))
                .collect(Collectors.toList());
    }

    /**
     * Gets all unique service names in this trace.
     * 
     * @return set of service names
     */
    public List<String> getAllServiceNames() {
        if (spans == null || processes == null) {
            return new ArrayList<>();
        }

        return spans.stream()
                .map(span -> getServiceName(span))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Sorts spans by start time.
     * 
     * @return sorted list of spans
     */
    public List<Span> getSpansSortedByTime() {
        if (spans == null)
            return new ArrayList<>();

        return spans.stream()
                .sorted((s1, s2) -> Long.compare(s1.getStartTime(), s2.getStartTime()))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "Trace{" +
                "traceID='" + traceID + '\'' +
                ", spans=" + (spans != null ? spans.size() : 0) +
                ", processes=" + (processes != null ? processes.size() : 0) +
                '}';
    }
}
