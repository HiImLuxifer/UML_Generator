package com.uml.generator.generator;

import com.uml.generator.model.Trace;
import java.util.List;

/**
 * Interface for UML diagram generators.
 */
public interface DiagramGenerator {

    /**
     * Generates PlantUML source code from traces.
     * 
     * @param traces list of traces to analyze
     * @return PlantUML source code
     */
    String generatePlantUML(List<Trace> traces);

    /**
     * Gets the diagram type name.
     * 
     * @return diagram type (e.g., "sequence", "component", "deployment")
     */
    String getDiagramType();
}
