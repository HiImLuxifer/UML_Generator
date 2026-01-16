package com.uml.generator.generator;

import com.uml.generator.analyzer.TraceAggregator;
import com.uml.generator.model.Trace;
import com.uml.generator.renderer.XmiWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.*;

/**
 * Generates UML Deployment Diagrams in XMI 2.5.1 format from aggregated Jaeger
 * traces.
 * Extracts deployment information from span tags (hostname, pod, namespace,
 * etc.)
 */
public class DeploymentDiagramGenerator implements DiagramGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentDiagramGenerator.class);
    private static final String UML_NAMESPACE = "http://www.eclipse.org/uml2/5.0.0/UML";

    @Override
    public String getDiagramType() {
        return "deployment";
    }

    @Override
    public String generateXmi(List<Trace> traces) {
        if (traces == null || traces.isEmpty()) {
            logger.warn("No traces provided for deployment diagram generation");
            return "";
        }

        try {
            TraceAggregator aggregator = new TraceAggregator(traces);
            XmiWriter writer = new XmiWriter();

            // Determine model name
            String modelName = "DeploymentDiagram";
            if (!traces.isEmpty() && traces.get(0).getSourceName() != null) {
                modelName = traces.get(0).getSourceName() + "_Deployment";
            }

            // Create XMI document
            Document doc = writer.createXmiDocument(modelName);
            Element model = writer.getModelElement(doc);

            // Extract deployment information from service metadata
            Map<String, Map<String, Object>> serviceMetadata = aggregator.getServiceMetadata();
            Map<String, Set<String>> nodeServices = new HashMap<>();
            Map<String, String> nodeIds = new HashMap<>();

            // Analyze metadata to extract deployment info
            for (Map.Entry<String, Map<String, Object>> entry : serviceMetadata.entrySet()) {
                String service = entry.getKey();
                Map<String, Object> metadata = entry.getValue();

                String node = extractNode(metadata);
                nodeServices.computeIfAbsent(node, k -> new HashSet<>()).add(service);
            }

            // Create nodes and deploy artifacts
            for (Map.Entry<String, Set<String>> nodeEntry : nodeServices.entrySet()) {
                String nodeName = nodeEntry.getKey();
                Set<String> services = nodeEntry.getValue();

                // Create node
                String nodeId = createNode(doc, model, nodeName);
                nodeIds.put(nodeName, nodeId);

                // Create artifacts and deployments for services on this node
                for (String service : services) {
                    String artifactId = createArtifact(doc, model, service);
                    createDeployment(doc, model, nodeName + "_deploys_" + service,
                            nodeId, artifactId);
                }
            }

            // Create communication paths between nodes
            Map<String, Set<String>> dependencies = aggregator.getServiceDependencies();
            Set<String> createdPaths = new HashSet<>();

            for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
                String fromService = entry.getKey();
                Set<String> toServices = entry.getValue();

                // Find nodes for services
                String fromNode = findNodeForService(fromService, nodeServices);

                for (String toService : toServices) {
                    String toNode = findNodeForService(toService, nodeServices);

                    if (fromNode != null && toNode != null && !fromNode.equals(toNode)) {
                        String pathKey = fromNode + "_to_" + toNode;
                        String pathKeyReverse = toNode + "_to_" + fromNode;

                        // Create bidirectional path only once
                        if (!createdPaths.contains(pathKey) && !createdPaths.contains(pathKeyReverse)) {
                            String fromNodeId = nodeIds.get(fromNode);
                            String toNodeId = nodeIds.get(toNode);

                            if (fromNodeId != null && toNodeId != null) {
                                createCommunicationPath(doc, model, pathKey, fromNodeId, toNodeId);
                                createdPaths.add(pathKey);
                            }
                        }
                    }
                }
            }

            int totalNodes = nodeServices.size();
            int totalServices = serviceMetadata.size();

            logger.info("Generated deployment diagram XMI with {} node(s) and {} service(s)",
                    totalNodes, totalServices);

            return writer.documentToString(doc);

        } catch (Exception e) {
            logger.error("Failed to generate deployment diagram XMI", e);
            return "";
        }
    }

    /**
     * Creates a UML Node element.
     */
    private String createNode(Document doc, Element model, String nodeName) {
        Element node = doc.createElementNS(UML_NAMESPACE, "packagedElement");
        node.setAttribute("xmi:type", "uml:Node");
        String nodeId = XmiWriter.generateUUID();
        node.setAttribute("xmi:id", nodeId);
        node.setAttribute("name", nodeName);

        model.appendChild(node);
        return nodeId;
    }

    /**
     * Creates a UML Artifact element.
     */
    private String createArtifact(Document doc, Element model, String artifactName) {
        Element artifact = doc.createElementNS(UML_NAMESPACE, "packagedElement");
        artifact.setAttribute("xmi:type", "uml:Artifact");
        String artifactId = XmiWriter.generateUUID();
        artifact.setAttribute("xmi:id", artifactId);
        artifact.setAttribute("name", artifactName);

        model.appendChild(artifact);
        return artifactId;
    }

    /**
     * Creates a UML Deployment relationship.
     */
    private void createDeployment(Document doc, Element model, String deploymentName,
            String nodeId, String artifactId) {
        Element deployment = doc.createElementNS(UML_NAMESPACE, "packagedElement");
        deployment.setAttribute("xmi:type", "uml:Deployment");
        deployment.setAttribute("xmi:id", XmiWriter.generateUUID());
        deployment.setAttribute("name", deploymentName);
        deployment.setAttribute("location", nodeId);
        deployment.setAttribute("deployedArtifact", artifactId);

        model.appendChild(deployment);
    }

    /**
     * Creates a UML CommunicationPath between two nodes.
     */
    private void createCommunicationPath(Document doc, Element model, String pathName,
            String sourceNodeId, String targetNodeId) {
        Element commPath = doc.createElementNS(UML_NAMESPACE, "packagedElement");
        commPath.setAttribute("xmi:type", "uml:CommunicationPath");
        commPath.setAttribute("xmi:id", XmiWriter.generateUUID());
        commPath.setAttribute("name", pathName);

        // Create member ends
        Element memberEnd1 = doc.createElementNS(UML_NAMESPACE, "memberEnd");
        memberEnd1.setAttribute("xmi:idref", sourceNodeId);
        commPath.appendChild(memberEnd1);

        Element memberEnd2 = doc.createElementNS(UML_NAMESPACE, "memberEnd");
        memberEnd2.setAttribute("xmi:idref", targetNodeId);
        commPath.appendChild(memberEnd2);

        model.appendChild(commPath);
    }

    /**
     * Finds the node where a service is deployed.
     */
    private String findNodeForService(String service, Map<String, Set<String>> nodeServices) {
        for (Map.Entry<String, Set<String>> entry : nodeServices.entrySet()) {
            if (entry.getValue().contains(service)) {
                return entry.getKey();
            }
        }
        return null;
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
}
