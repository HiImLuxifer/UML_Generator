package com.uml.generator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a reference between spans in Jaeger.
 * References indicate relationships like CHILD_OF or FOLLOWS_FROM.
 */
public class Reference {

    @JsonProperty("refType")
    private String refType;

    @JsonProperty("traceID")
    private String traceID;

    @JsonProperty("spanID")
    private String spanID;

    public Reference() {
    }

    public Reference(String refType, String traceID, String spanID) {
        this.refType = refType;
        this.traceID = traceID;
        this.spanID = spanID;
    }

    public String getRefType() {
        return refType;
    }

    public void setRefType(String refType) {
        this.refType = refType;
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

    /**
     * Checks if this is a CHILD_OF reference.
     * 
     * @return true if this is a CHILD_OF reference
     */
    public boolean isChildOf() {
        return "CHILD_OF".equalsIgnoreCase(refType);
    }

    /**
     * Checks if this is a FOLLOWS_FROM reference.
     * 
     * @return true if this is a FOLLOWS_FROM reference
     */
    public boolean isFollowsFrom() {
        return "FOLLOWS_FROM".equalsIgnoreCase(refType);
    }

    @Override
    public String toString() {
        return "Reference{" +
                "refType='" + refType + '\'' +
                ", traceID='" + traceID + '\'' +
                ", spanID='" + spanID + '\'' +
                '}';
    }
}
