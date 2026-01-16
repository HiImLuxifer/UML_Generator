package com.uml.generator.generator;

import com.uml.generator.model.Trace;
import java.util.List;

/**
 * Interface for UML diagram generators.
 */
public interface DiagramGenerator {

    /**
     * Generates XMI (XML Metadata Interchange) content from traces.
     * Conforms to OMG XMI 2.5.1 and UML 2.5.1 specifications.
     * 
     * @param traces list of traces to analyze
     * @return XMI XML content as string
     */
    String generateXmi(List<Trace> traces);

    /**
     * Gets the diagram type name.
     * 
     * @return diagram type (e.g., "sequence", "component", "deployment")
     */
    String getDiagramType();
}
