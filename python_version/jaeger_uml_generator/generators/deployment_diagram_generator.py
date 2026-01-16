"""Deployment diagram generator."""

import logging
import xml.etree.ElementTree as ET
from typing import List, Dict, Set
from .diagram_generator import DiagramGenerator
from ..models import Trace
from ..analyzer import TraceAggregator
from ..renderer import XmiWriter, XmiFormat
from ..utils import extract_base_name


logger = logging.getLogger(__name__)


class DeploymentDiagramGenerator(DiagramGenerator):
    """Generates UML Deployment Diagrams in XMI 2.5.1 format from aggregated Jaeger traces."""
    
    def __init__(self, xmi_format: str = "papyrus"):
        """Initialize generator with XMI format.
        
        Args:
            xmi_format: Output format ('papyrus' or 'magicdraw')
        """
        format_enum = XmiFormat(xmi_format)
        self.xmi_writer = XmiWriter(format_enum)
    
    def get_diagram_type(self) -> str:
        return "deployment"
    
    def generate_xmi(self, traces: List[Trace]) -> str:
        """
        Generate XMI for deployment diagram from multiple traces.
        
        Args:
            traces: List of Trace objects
            
        Returns:
            XMI content as string
        """
        if not traces:
            logger.warning("No traces provided for deployment diagram generation")
            return ""
        
        try:
            aggregator = TraceAggregator(traces)
            
            # Determine model name
            model_name = "DeploymentDiagram"
            if traces and traces[0].source_name:
                model_name = f"{traces[0].source_name}_Deployment"
            
            # Create XMI document
            root = self.xmi_writer.create_xmi_document(model_name)
            model = self.xmi_writer.get_model_element(root)
            
            # Extract deployment information from service metadata
            service_metadata = aggregator.get_service_metadata()
            node_services: Dict[str, Set[str]] = {}
            node_ids: Dict[str, str] = {}
            
            # Analyze metadata to extract deployment info
            for service, metadata in service_metadata.items():
                node = self._extract_node(metadata)
                if node not in node_services:
                    node_services[node] = set()
                node_services[node].add(service)
            
            # Create nodes and deploy artifacts
            artifact_ids: Dict[str, str] = {}
            
            for node_name, services in node_services.items():
                # Create node
                node_elem, node_id = self.xmi_writer.create_packaged_element(
                    model, "Node", node_name
                )
                node_ids[node_name] = node_id
                
                # Create artifacts and deployments for services on this node
                for service in services:
                    artifact_elem, artifact_id = self.xmi_writer.create_packaged_element(
                        model, "Artifact", service
                    )
                    artifact_ids[service] = artifact_id
                    
                    self._create_deployment(
                        model,
                        f"{node_name}_deploys_{service}",
                        node_id,
                        artifact_id
                    )
            
            # Create communication paths between nodes
            dependencies = aggregator.get_service_dependencies()
            created_paths: Set[str] = set()
            
            for from_service, to_services in dependencies.items():
                # Find node for from_service
                from_node = self._find_node_for_service(from_service, node_services)
                
                for to_service in to_services:
                    to_node = self._find_node_for_service(to_service, node_services)
                    
                    if from_node and to_node and from_node != to_node:
                        path_key = f"{from_node}_to_{to_node}"
                        path_key_reverse = f"{to_node}_to_{from_node}"
                        
                        # Create bidirectional path only once
                        if path_key not in created_paths and path_key_reverse not in created_paths:
                            from_node_id = node_ids.get(from_node)
                            to_node_id = node_ids.get(to_node)
                            
                            if from_node_id and to_node_id:
                                self._create_communication_path(
                                    model,
                                    path_key,
                                    from_node_id,
                                    to_node_id
                                )
                                created_paths.add(path_key)
            
            total_nodes = len(node_services)
            total_services = len(service_metadata)
            
            logger.info(f"Generated deployment diagram XMI with {total_nodes} node(s) "
                       f"and {total_services} service(s)")
            
            return self.xmi_writer.document_to_string(root)
            
        except Exception as e:
            logger.error(f"Failed to generate deployment diagram XMI: {e}")
            return ""
    
    def _create_deployment(self, model: ET.Element, deployment_name: str,
                          node_id: str, artifact_id: str):
        """
        Create a UML Deployment relationship.
        
        Args:
            model: Parent model element
            deployment_name: Deployment name
            node_id: Node ID where artifact is deployed
            artifact_id: Artifact ID
        """
        self.xmi_writer.create_packaged_element(
            model, "Deployment", deployment_name,
            location=node_id, deployedArtifact=artifact_id
        )
    
    def _create_communication_path(self, model: ET.Element, path_name: str,
                                   source_node_id: str, target_node_id: str):
        """
        Create a UML CommunicationPath between two nodes.
        
        Args:
            model: Parent model element
            path_name: Path name
            source_node_id: Source node ID
            target_node_id: Target node ID
        """
        comm_path, _ = self.xmi_writer.create_packaged_element(
            model, "CommunicationPath", path_name
        )
        
        # Create member ends (using ownedEnd instead of memberEnd for better compatibility)
        end1 = ET.SubElement(comm_path, "ownedEnd")
        end1.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}id", self.xmi_writer.generate_uuid())
        end1.set("type", source_node_id)
        
        end2 = ET.SubElement(comm_path, "ownedEnd")
        end2.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}id", self.xmi_writer.generate_uuid())
        end2.set("type", target_node_id)
    
    def _find_node_for_service(self, service: str, 
                               node_services: Dict[str, Set[str]]) -> str:
        """
        Find the node where a service is deployed.
        
        Args:
            service: Service name
            node_services: Map of node names to services
            
        Returns:
            Node name or None
        """
        for node_name, services in node_services.items():
            if service in services:
                return node_name
        return None
    
    def _extract_node(self, metadata: Dict[str, any]) -> str:
        """
        Extract node/host information from metadata tags.
        Supports multiple deployment platforms: Kubernetes, Docker, VMs, Cloud.
        
        Args:
            metadata: Service metadata dictionary
            
        Returns:
            Node name
        """
        if not metadata:
            return "Node-Unknown"
        
        # Kubernetes: Try pod.name, k8s.pod.name
        pod_name = metadata.get('pod.name') or metadata.get('k8s.pod.name')
        if pod_name:
            # Extract base name (remove K8s hash suffix)
            return extract_base_name(str(pod_name))
        
        # Docker: Try container.name, container.id
        container_name = metadata.get('container.name')
        if container_name:
            return extract_base_name(str(container_name))
        
        container_id = metadata.get('container.id')
        if container_id:
            # Use short form of container ID (first 12 chars)
            container_id_str = str(container_id)
            if len(container_id_str) > 12:
                container_id_str = container_id_str[:12]
            return f"Container-{container_id_str}"
        
        # VM/Cloud: Try hostname, instance.id, host.name, node.name
        hostname = metadata.get('hostname')
        if hostname:
            return str(hostname)
        
        instance_id = metadata.get('instance.id')
        if instance_id:
            return str(instance_id)
        
        host_name = metadata.get('host.name')
        if host_name:
            return str(host_name)
        
        node_name = metadata.get('node.name')
        if node_name:
            return str(node_name)
        
        host_ip = metadata.get('host.ip')
        if host_ip:
            return f"Host-{host_ip}"
        
        # Generic fallback
        return f"Node-{abs(hash(str(metadata)) % 10000)}"
