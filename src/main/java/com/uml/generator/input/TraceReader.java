package com.uml.generator.input;

import com.uml.generator.model.Trace;
import java.util.List;

/**
 * Interface for reading Jaeger traces from various sources.
 */
public interface TraceReader {

    /**
     * Reads traces from the configured source.
     * 
     * @return list of traces
     * @throws Exception if reading fails
     */
    List<Trace> readTraces() throws Exception;
}
