package com.uml.generator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a Jaeger process (service).
 * Each process has a service name and optional tags with metadata.
 */
public class Process {

    @JsonProperty("serviceName")
    private String serviceName;

    @JsonProperty("tags")
    private List<Map<String, Object>> tags;

    public Process() {
        this.tags = new ArrayList<>();
    }

    public Process(String serviceName, List<Map<String, Object>> tags) {
        this.serviceName = serviceName;
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public List<Map<String, Object>> getTags() {
        return tags;
    }

    public void setTags(List<Map<String, Object>> tags) {
        this.tags = tags;
    }

    /**
     * Gets a tag value by key.
     * 
     * @param key the tag key
     * @return the tag value or null if not found
     */
    public Object getTag(String key) {
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
    public String getTagAsString(String key) {
        Object value = getTag(key);
        return value != null ? value.toString() : null;
    }

    @Override
    public String toString() {
        return "Process{" +
                "serviceName='" + serviceName + '\'' +
                ", tags=" + (tags != null ? tags.size() + " tag(s)" : "0") +
                '}';
    }
}
