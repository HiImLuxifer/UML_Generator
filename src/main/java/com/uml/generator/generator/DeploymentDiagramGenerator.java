package com.uml.generator.generator;

import com.uml.generator.analyzer.TraceAggregator;
import com.uml.generator.model.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Generates UML Deployment Diagrams from aggregated Jaeger traces.
 * Extracts deployment information from span tags (hostname, pod, namespace,
 * etc.)
 */
public class DeploymentDiagramGenerator implements DiagramGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentDiagramGenerator.class);

    @Override
    public String getDiagramType() {
        return "deployment";
    }

    @Override
    public String generatePlantUML(List<Trace> traces) {
        if (traces == null || traces.isEmpty()) {
            logger.warn("No traces provided for deployment diagram generation");
            return "";
        }

        TraceAggregator aggregator = new TraceAggregator(traces);

        StringBuilder puml = new StringBuilder();

        puml.append("@startuml\n");
        puml.append("title Deployment Diagram - Infrastructure\n\n");

        // Extract deployment information from service metadata
        Map<String, Map<String, Object>> serviceMetadata = aggregator.getServiceMetadata();
        Map<String, Set<String>> nodeServices = new HashMap<>();
        Set<String> namespaces = new HashSet<>();

        // Analyze metadata to extract deployment info
        for (Map.Entry<String, Map<String, Object>> entry : serviceMetadata.entrySet()) {
            String service = entry.getKey();
            Map<String, Object> metadata = entry.getValue();

            String node = extractNode(metadata);
            String namespace = extractNamespace(metadata);

            if (namespace != null) {
                namespaces.add(namespace);
            }

            nodeServices.computeIfAbsent(node, k -> new HashSet<>()).add(service);
        }

        // Generate deployment diagram
        if (!namespaces.isEmpty()) {
            // If we have namespace info, use it
            for (String namespace : namespaces) {
                puml.append("node \"Namespace: ").append(namespace).append("\" {\n");

                for (Map.Entry<String, Set<String>> entry : nodeServices.entrySet()) {
                    String node = entry.getKey();
                    Set<String> services = entry.getValue();

                    puml.append("  node \"").append(node).append("\" {\n");

                    for (String service : services) {
                        puml.append("    artifact \"").append(service).append("\"\n");
                    }

                    puml.append("  }\n");
                }

                puml.append("}\n\n");
            }
        } else {
            // Simple deployment without namespace grouping
            for (Map.Entry<String, Set<String>> entry : nodeServices.entrySet()) {
                String node = entry.getKey();
                Set<String> services = entry.getValue();

                puml.append("node \"").append(node).append("\" {\n");

                for (String service : services) {
                    puml.append("  artifact \"").append(service).append("\"\n");
                }

                puml.append("}\n\n");
            }
        }

        // Add dependencies if available
        Map<String, Set<String>> dependencies = aggregator.getServiceDependencies();
        if (!dependencies.isEmpty()) {
            puml.append("' Service dependencies\n");
            for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
                String fromService = entry.getKey();
                Set<String> toServices = entry.getValue();

                for (String toService : toServices) {
                    puml.append("\"").append(fromService).append("\" --> \"")
                            .append(toService).append("\"\n");
                }
            }
        }

        puml.append("\n@enduml");

        logger.info("Generated deployment diagram with {} node(s)", nodeServices.size());

        return puml.toString();
    }

    /**
     * Extracts node/host information from metadata tags.
     * Supports multiple deployment platforms: Kubernetes, Docker, VMs, Cloud.
     */
    private String extractNode(Map<String, Object> metadata) {
        if (metadata == null)
            return "Node-Unknown";

        String nodeName = null;

        // Kubernetes: Try pod.name, k8s.pod.name
        Object podName = metadata.get("pod.name");
        if (podName == null) {
            podName = metadata.get("k8s.pod.name");
        }
        if (podName != null) {
            nodeName = podName.toString();
            // Extract base name (remove K8s hash suffix)
            nodeName = com.uml.generator.util.NameUtils.extractBaseName(nodeName);
            return nodeName;
        }

        // Docker: Try container.name, container.id
        Object containerName = metadata.get("container.name");
        if (containerName != null) {
            nodeName = containerName.toString();
            nodeName = com.uml.generator.util.NameUtils.extractBaseName(nodeName);
            return nodeName;
        }

        Object containerId = metadata.get("container.id");
        if (containerId != null) {
            String id = containerId.toString();
            // Use short form of container ID (first 12 chars)
            if (id.length() > 12) {
                id = id.substring(0, 12);
            }
            return "Container-" + id;
        }

        // VM/Cloud: Try hostname, instance.id, host.name, node.name
        Object hostname = metadata.get("hostname");
        if (hostname != null) {
            return hostname.toString();
        }

        Object instanceId = metadata.get("instance.id");
        if (instanceId != null) {
            return instanceId.toString();
        }

        Object hostName = metadata.get("host.name");
        if (hostName != null) {
            return hostName.toString();
        }

        Object nodeNameObj = metadata.get("node.name");
        if (nodeNameObj != null) {
            return nodeNameObj.toString();
        }

        Object hostIp = metadata.get("host.ip");
        if (hostIp != null) {
            return "Host-" + hostIp.toString();
        }

        // Generic fallback
        return "Node-" + Math.abs(metadata.hashCode() % 10000);
    }

    /**
     * Extracts namespace information from metadata tags.
     */
    private String extractNamespace(Map<String, Object> metadata) {
        if (metadata == null)
            return null;

        Object namespace = metadata.get("namespace");
        if (namespace != null)
            return namespace.toString();

        Object k8sNamespace = metadata.get("k8s.namespace");
        if (k8sNamespace != null)
            return k8sNamespace.toString();

        return null;
    }
}
