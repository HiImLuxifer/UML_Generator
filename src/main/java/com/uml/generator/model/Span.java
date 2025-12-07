package com.uml.generator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a Jaeger span.
 * A span represents a single operation within a distributed trace.
 */
public class Span {

    @JsonProperty("traceID")
    private String traceID;

    @JsonProperty("spanID")
    private String spanID;

    @JsonProperty("operationName")
    private String operationName;

    @JsonProperty("references")
    private List<Reference> references;

    @JsonProperty("startTime")
    private long startTime;

    @JsonProperty("duration")
    private long duration;

    @JsonProperty("tags")
    private List<Map<String, Object>> tags;

    @JsonProperty("logs")
    private List<Map<String, Object>> logs;

    @JsonProperty("processID")
    private String processID;

    @JsonProperty("warnings")
    private List<String> warnings;

    public Span() {
        this.references = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.logs = new ArrayList<>();
    }

    public String getTraceID() {
        return traceID;
    }

    public void setTraceID(String traceID) {
        this.traceID = traceID;
    }

    public String getSpanID() {
        return spanID;
    }

    public void setSpanID(String spanID) {
        this.spanID = spanID;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public List<Reference> getReferences() {
        return references;
    }

    public void setReferences(List<Reference> references) {
        this.references = references;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public List<Map<String, Object>> getTags() {
        return tags;
    }

    public void setTags(List<Map<String, Object>> tags) {
        this.tags = tags;
    }

    public List<Map<String, Object>> getLogs() {
        return logs;
    }

    public void setLogs(List<Map<String, Object>> logs) {
        this.logs = logs;
    }

    public String getProcessID() {
        return processID;
    }

    public void setProcessID(String processID) {
        this.processID = processID;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    /**
     * Gets a tag value by key.
     * 
     * @param key the tag key
     * @return the tag value or null if not found
     */
    public Object getTagValue(String key) {
        if (tags == null)
            return null;

        for (Map<String, Object> tag : tags) {
            if (key.equals(tag.get("key"))) {
                return tag.get("value");
            }
        }
        return null;
    }

    /**
     * Gets a tag value by key as a String.
     * 
     * @param key the tag key
     * @return the tag value as String or null if not found
     */
    public String getTagValueAsString(String key) {
        Object value = getTagValue(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Gets the parent span ID if this span has a CHILD_OF reference.
     * 
     * @return the parent span ID or null
     */
    public String getParentSpanId() {
        if (references == null)
            return null;

        for (Reference ref : references) {
            if (ref.isChildOf()) {
                return ref.getSpanID();
            }
        }
        return null;
    }

    /**
     * Checks if this is a root span (no parent).
     * 
     * @return true if this span has no parent
     */
    public boolean isRootSpan() {
        return getParentSpanId() == null;
    }

    @Override
    public String toString() {
        return "Span{" +
                "spanID='" + spanID + '\'' +
                ", operationName='" + operationName + '\'' +
                ", processID='" + processID + '\'' +
                ", duration=" + duration +
                '}';
    }
}
