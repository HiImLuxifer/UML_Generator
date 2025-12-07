package com.uml.generator.generator;

import com.uml.generator.analyzer.TraceAggregator;
import com.uml.generator.model.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates UML Component Diagrams from aggregated Jaeger traces.
 * Shows services as components with their interfaces and dependencies.
 */
public class ComponentDiagramGenerator implements DiagramGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ComponentDiagramGenerator.class);

    @Override
    public String getDiagramType() {
        return "component";
    }

    @Override
    public String generatePlantUML(List<Trace> traces) {
        if (traces == null || traces.isEmpty()) {
            logger.warn("No traces provided for component diagram generation");
            return "";
        }

        TraceAggregator aggregator = new TraceAggregator(traces);

        StringBuilder puml = new StringBuilder();

        puml.append("@startuml\n");
        puml.append("title Component Diagram - System Architecture\n\n");

        // Get all services and their relationships
        Set<String> services = aggregator.getAllServices();
        Map<String, Map<String, Set<String>>> serviceCalls = aggregator.getServiceCalls();

        // Define all components
        for (String service : services) {
            puml.append("component \"").append(service).append("\"\n");
        }
        puml.append("\n");

        // Define dependencies between components (simplified, no individual interfaces)
        puml.append("' Service dependencies\n");
        for (Map.Entry<String, Map<String, Set<String>>> callerEntry : serviceCalls.entrySet()) {
            String callerService = callerEntry.getKey();
            Map<String, Set<String>> calleeMap = callerEntry.getValue();

            for (Map.Entry<String, Set<String>> calleeEntry : calleeMap.entrySet()) {
                String calleeService = calleeEntry.getKey();

                // Simple dependency arrow without operation details
                puml.append("\"").append(callerService).append("\" --> \"")
                        .append(calleeService).append("\"\n");
            }
        }

        puml.append("\n@enduml");

        logger.info("Generated component diagram with {} service(s)", services.size());

        return puml.toString();
    }
}
