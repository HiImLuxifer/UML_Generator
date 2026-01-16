package com.uml.generator.generator;

import com.uml.generator.analyzer.TraceAggregator;
import com.uml.generator.model.Trace;
import com.uml.generator.renderer.XmiWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates UML Component Diagrams in XMI 2.5.1 format from aggregated Jaeger
 * traces.
 * Shows services as components with their interfaces and dependencies.
 */
public class ComponentDiagramGenerator implements DiagramGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ComponentDiagramGenerator.class);
    private static final String UML_NAMESPACE = "http://www.eclipse.org/uml2/5.0.0/UML";

    @Override
    public String getDiagramType() {
        return "component";
    }

    @Override
    public String generateXmi(List<Trace> traces) {
        if (traces == null || traces.isEmpty()) {
            logger.warn("No traces provided for component diagram generation");
            return "";
        }

        try {
            TraceAggregator aggregator = new TraceAggregator(traces);
            XmiWriter writer = new XmiWriter();

            // Determine model name
            String modelName = "ComponentDiagram";
            if (!traces.isEmpty() && traces.get(0).getSourceName() != null) {
                modelName = traces.get(0).getSourceName() + "_Component";
            }

            // Create XMI document
            Document doc = writer.createXmiDocument(modelName);
            Element model = writer.getModelElement(doc);

            // Get all services and their relationships
            Set<String> services = aggregator.getAllServices();
            Map<String, Map<String, Object>> serviceMetadata = aggregator.getServiceMetadata();
            Map<String, Map<String, Set<String>>> serviceCalls = aggregator.getServiceCalls();

            // Track created elements to reference them
            Map<String, String> componentIds = new HashMap<>();
            Map<String, String> interfaceIds = new HashMap<>();

            // Create components for all services
            for (String service : services) {
                Map<String, Object> metadata = serviceMetadata.get(service);
                String componentId = createComponent(doc, model, service, metadata);
                componentIds.put(service, componentId);
            }

            // Create interfaces and dependencies
            int interfaceCounter = 0;
            int totalDependencies = 0;

            for (Map.Entry<String, Map<String, Set<String>>> callerEntry : serviceCalls.entrySet()) {
                String callerService = callerEntry.getKey();
                Map<String, Set<String>> calleeMap = callerEntry.getValue();

                for (Map.Entry<String, Set<String>> calleeEntry : calleeMap.entrySet()) {
                    String calleeService = calleeEntry.getKey();
                    Set<String> operations = calleeEntry.getValue();

                    // Create interface for callee service
                    String interfaceName = calleeService + "API";
                    String interfaceId;

                    // Check if interface already exists
                    if (interfaceIds.containsKey(interfaceName)) {
                        interfaceId = interfaceIds.get(interfaceName);
                    } else {
                        interfaceId = createInterface(doc, model, interfaceName, operations);
                        interfaceIds.put(interfaceName, interfaceId);
                    }

                    // Create dependency from caller to interface
                    String callerId = componentIds.get(callerService);
                    if (callerId != null && interfaceId != null) {
                        createDependency(doc, model, callerService + "_uses_" + interfaceName,
                                callerId, interfaceId);
                        totalDependencies++;
                    }

                    // Create interface realization from callee to interface
                    String calleeId = componentIds.get(calleeService);
                    if (calleeId != null && interfaceId != null) {
                        createInterfaceRealization(doc, model, calleeService + "_provides_" + interfaceName,
                                calleeId, interfaceId);
                    }
                }
            }

            logger.info("Generated component diagram XMI with {} service(s) and {} dependencies",
                    services.size(), totalDependencies);

            // Convert to string
            return writer.documentToString(doc);

        } catch (Exception e) {
            logger.error("Failed to generate component diagram XMI", e);
            return "";
        }
    }

    /**
     * Creates a UML Component element.
     */
    private String createComponent(Document doc, Element model, String serviceName, Map<String, Object> metadata) {
        Element component = doc.createElementNS(UML_NAMESPACE, "packagedElement");
        component.setAttribute("xmi:type", "uml:Component");
        String componentId = XmiWriter.generateUUID();
        component.setAttribute("xmi:id", componentId);
        component.setAttribute("name", serviceName);

        // Add stereotype based on service type
        String stereotype = detectServiceStereotype(serviceName, metadata);
        if (stereotype != null && !stereotype.isEmpty()) {
            // Store stereotype as a tagged value comment for now
            // Full stereotype support would require UML Profile extension
            component.setAttribute("visibility", "public");
            // Add comment to indicate stereotype
            addComment(doc, component, "Stereotype: " + stereotype);
        }

        model.appendChild(component);
        return componentId;
    }

    /**
     * Creates a UML Interface element with operations.
     */
    private String createInterface(Document doc, Element model, String interfaceName, Set<String> operations) {
        Element interfaceElement = doc.createElementNS(UML_NAMESPACE, "packagedElement");
        interfaceElement.setAttribute("xmi:type", "uml:Interface");
        String interfaceId = XmiWriter.generateUUID();
        interfaceElement.setAttribute("xmi:id", interfaceId);
        interfaceElement.setAttribute("name", interfaceName);

        // Add operations to interface (limit to 10 for readability)
        if (operations != null && !operations.isEmpty()) {
            int count = 0;
            for (String operation : operations) {
                if (count >= 10)
                    break;
                createOperation(doc, interfaceElement, operation);
                count++;
            }

            // Add comment if there are more operations
            if (operations.size() > 10) {
                addComment(doc, interfaceElement,
                        "+" + (operations.size() - 10) + " more operations not shown");
            }
        }

        model.appendChild(interfaceElement);
        return interfaceId;
    }

    /**
     * Creates a UML Operation element.
     */
    private void createOperation(Document doc, Element interfaceElement, String operationName) {
        Element operation = doc.createElementNS(UML_NAMESPACE, "ownedOperation");
        operation.setAttribute("xmi:id", XmiWriter.generateUUID());

        // Clean operation name
        String cleanName = com.uml.generator.util.NameUtils.cleanOperationName(operationName);
        operation.setAttribute("name", cleanName);
        operation.setAttribute("visibility", "public");

        interfaceElement.appendChild(operation);
    }

    /**
     * Creates a UML Dependency element.
     */
    private void createDependency(Document doc, Element model, String name, String clientId, String supplierId) {
        Element dependency = doc.createElementNS(UML_NAMESPACE, "packagedElement");
        dependency.setAttribute("xmi:type", "uml:Dependency");
        dependency.setAttribute("xmi:id", XmiWriter.generateUUID());
        dependency.setAttribute("name", name);
        dependency.setAttribute("client", clientId);
        dependency.setAttribute("supplier", supplierId);

        model.appendChild(dependency);
    }

    /**
     * Creates a UML InterfaceRealization element.
     */
    private void createInterfaceRealization(Document doc, Element model, String name,
            String implementingClassifierId, String contractId) {
        Element realization = doc.createElementNS(UML_NAMESPACE, "packagedElement");
        realization.setAttribute("xmi:type", "uml:InterfaceRealization");
        realization.setAttribute("xmi:id", XmiWriter.generateUUID());
        realization.setAttribute("name", name);
        realization.setAttribute("implementingClassifier", implementingClassifierId);
        realization.setAttribute("contract", contractId);

        model.appendChild(realization);
    }

    /**
     * Adds a comment element to a UML element.
     */
    private void addComment(Document doc, Element element, String commentText) {
        Element comment = doc.createElementNS(UML_NAMESPACE, "ownedComment");
        comment.setAttribute("xmi:id", XmiWriter.generateUUID());
        comment.setAttribute("annotatedElement", element.getAttribute("xmi:id"));

        Element body = doc.createElementNS(UML_NAMESPACE, "body");
        body.setTextContent(commentText);
        comment.appendChild(body);

        element.appendChild(comment);
    }

    /**
     * Detects the stereotype for a service based on its name and metadata.
     */
    private String detectServiceStereotype(String serviceName, Map<String, Object> metadata) {
        if (serviceName == null) {
            return null;
        }

        String lowerName = serviceName.toLowerCase();

        // Check for frontend/UI services
        if (lowerName.contains("frontend") || lowerName.contains("ui") ||
                lowerName.contains("web") || lowerName.contains("client")) {
            return "WebUI";
        }

        // Check for database services
        if (lowerName.contains("database") || lowerName.contains("db") ||
                lowerName.contains("mongo") || lowerName.contains("postgres") ||
                lowerName.contains("mysql") || lowerName.contains("redis")) {
            return "Database";
        }

        // Check for cache services
        if (lowerName.contains("cache") || lowerName.contains("redis") ||
                lowerName.contains("memcache")) {
            return "Cache";
        }

        // Check metadata for gRPC
        if (metadata != null) {
            Object rpcSystem = metadata.get("rpc.system");
            if (rpcSystem != null && rpcSystem.toString().toLowerCase().contains("grpc")) {
                return "gRPC";
            }
        }

        // Generic microservice for services ending in 'service'
        if (lowerName.endsWith("service")) {
            return "Microservice";
        }

        return null;
    }
}
