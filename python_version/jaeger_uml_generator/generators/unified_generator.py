"""Unified XMI generator that creates all diagrams in a single file."""

import logging
import xml.etree.ElementTree as ET
from typing import List, Dict, Set

from ..models import Trace
from ..analyzer import TraceAggregator
from ..renderer import XmiWriter, XmiFormat, MarteProfileWriter
from ..utils import extract_simple_operation_name, extract_base_name


logger = logging.getLogger(__name__)


class UnifiedXmiGenerator:
    """
    Generates a single XMI file containing all UML diagrams:
    - Component diagram with operations
    - Deployment diagram with manifestations
    - Sequence diagrams inside Use Cases
    
    All elements are cross-referenced using shared IDs.
    Includes MARTE profile stereotypes for performance analysis.
    """
    
    def __init__(self, xmi_format: str = "papyrus", include_marte: bool = True):
        """
        Initialize unified generator.
        
        Args:
            xmi_format: Output format ('papyrus' or 'magicdraw')
            include_marte: Whether to include MARTE profile annotations
        """
        format_enum = XmiFormat(xmi_format)
        self.xmi_writer = XmiWriter(format_enum)
        self.include_marte = include_marte
        
        # Initialize MARTE profile writer
        self.marte_writer = MarteProfileWriter(self.xmi_writer.XMI_NAMESPACE)
        
        # Shared element IDs across all diagrams
        self.component_ids: Dict[str, str] = {}  # service -> component_id
        self.operation_ids: Dict[str, Dict[str, str]] = {}  # service -> {op_name -> op_id}
        self.artifact_ids: Dict[str, str] = {}  # service -> artifact_id
        self.node_ids: Dict[str, str] = {}  # node_name -> node_id
        
        # IDs for MARTE stereotype applications
        self.interaction_ids: Dict[str, str] = {}  # trace_name -> interaction_id
        self.message_ids: List[tuple] = []  # [(message_id, duration_ms), ...]
    
    def generate(self, traces: List[Trace], model_name: str = "UnifiedModel") -> str:
        """
        Generate unified XMI from traces.
        
        Args:
            traces: List of Trace objects
            model_name: Name for the UML model
            
        Returns:
            XMI content as string
        """
        if not traces:
            logger.warning("No traces provided for unified XMI generation")
            return ""
        
        try:
            aggregator = TraceAggregator(traces)
            
            # Reset IDs for new generation
            self.component_ids.clear()
            self.operation_ids.clear()
            self.artifact_ids.clear()
            self.node_ids.clear()
            self.interaction_ids.clear()
            self.message_ids.clear()
            
            # Create XMI document
            root = self.xmi_writer.create_xmi_document(model_name)
            model = self.xmi_writer.get_model_element(root)
            
            # Add MARTE profile application to model
            if self.include_marte:
                self.marte_writer.add_profile_application(model)
            
            # Step 1: Generate Components with operations
            self._generate_components(model, aggregator)
            
            # Step 2: Generate Deployment (Nodes, Artifacts with manifestations)
            self._generate_deployment(model, aggregator)
            
            # Step 3: Generate Sequences inside Use Cases
            self._generate_sequences(model, traces)
            
            # Step 4: Apply MARTE stereotypes (after all elements are created)
            if self.include_marte:
                self._apply_marte_stereotypes(root)
            
            logger.info(f"Generated unified XMI with {len(self.component_ids)} components")
            if self.include_marte:
                logger.info(f"Applied MARTE stereotypes: {len(self.interaction_ids)} GaAnalysisContext, "
                           f"{len(self.message_ids)} PaStep, {len(self.node_ids)} GaExecHost, "
                           f"{len(self.component_ids)} RtUnit")
            
            return self.xmi_writer.document_to_string(root)
            
        except Exception as e:
            logger.error(f"Failed to generate unified XMI: {e}")
            import traceback
            traceback.print_exc()
            return ""
    
    def _generate_components(self, model: ET.Element, aggregator: TraceAggregator):
        """Generate Component diagram elements."""
        services = aggregator.get_all_services()
        service_metadata = aggregator.get_service_metadata()
        service_operations = aggregator.get_service_operations()
        service_calls = aggregator.get_service_calls()
        
        # Create Components package
        components_pkg, _ = self.xmi_writer.create_package(model, "Components")
        
        # Create components with operations
        for service in sorted(services):
            metadata = service_metadata.get(service, {})
            operations = service_operations.get(service, set())
            
            # Create component
            component, component_id = self.xmi_writer.create_packaged_element(
                components_pkg, "Component", service, visibility="public"
            )
            self.component_ids[service] = component_id
            self.operation_ids[service] = {}
            
            # Add operations
            for op_name in sorted(operations):
                clean_op = extract_simple_operation_name(op_name)
                op_elem, op_id = self.xmi_writer.create_owned_element(
                    component, "ownedOperation",
                    name=clean_op, visibility="public"
                )
                op_elem.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}type", "uml:Operation")
                self.operation_ids[service][clean_op] = op_id
        
        # Create dependencies between components
        deps_pkg, _ = self.xmi_writer.create_package(model, "Dependencies")
        created_deps: Set[str] = set()
        
        for caller, callee_map in service_calls.items():
            for callee in callee_map.keys():
                caller_id = self.component_ids.get(caller)
                callee_id = self.component_ids.get(callee)
                
                if caller_id and callee_id:
                    dep_key = f"{caller}_to_{callee}"
                    if dep_key not in created_deps:
                        self.xmi_writer.create_usage(
                            deps_pkg, dep_key, caller_id, callee_id
                        )
                        created_deps.add(dep_key)
        
        logger.info(f"Generated {len(self.component_ids)} components with operations")
    
    def _generate_deployment(self, model: ET.Element, aggregator: TraceAggregator):
        """Generate Deployment diagram elements with manifestations to components."""
        service_metadata = aggregator.get_service_metadata()
        
        # Create Deployment package
        deployment_pkg, _ = self.xmi_writer.create_package(model, "Deployment")
        
        # Group services by node
        node_services: Dict[str, Set[str]] = {}
        for service, metadata in service_metadata.items():
            node = self._extract_node(metadata)
            if node not in node_services:
                node_services[node] = set()
            node_services[node].add(service)
        
        # Create nodes
        for node_name in sorted(node_services.keys()):
            node_elem, node_id = self.xmi_writer.create_packaged_element(
                deployment_pkg, "Node", node_name
            )
            self.node_ids[node_name] = node_id
        
        # Create artifacts with manifestations to components
        for node_name, services in node_services.items():
            node_id = self.node_ids[node_name]
            
            for service in sorted(services):
                # Create artifact
                artifact, artifact_id = self.xmi_writer.create_packaged_element(
                    deployment_pkg, "Artifact", f"{service}_artifact"
                )
                self.artifact_ids[service] = artifact_id
                
                # Create Manifestation (Artifact manifests Component)
                component_id = self.component_ids.get(service)
                if component_id:
                    manifestation = ET.SubElement(artifact, "manifestation")
                    manifestation.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}type", "uml:Manifestation")
                    manifestation.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}id", self.xmi_writer.generate_uuid())
                    manifestation.set("name", f"manifest_{service}")
                    manifestation.set("supplier", component_id)
                    manifestation.set("client", artifact_id)
                
                # Create Deployment relationship (Node deploys Artifact)
                deployment = self.xmi_writer.create_packaged_element(
                    deployment_pkg, "Deployment", f"deploy_{service}",
                    location=node_id, deployedArtifact=artifact_id
                )
        
        logger.info(f"Generated deployment with {len(self.node_ids)} nodes and {len(self.artifact_ids)} artifacts")
    
    def _generate_sequences(self, model: ET.Element, traces: List[Trace]):
        """Generate Sequence diagrams inside Use Cases, referencing Components."""
        # Create UseCases package
        usecases_pkg, _ = self.xmi_writer.create_package(model, "UseCases")
        
        for i, trace in enumerate(traces):
            trace_name = trace.source_name if trace.source_name else f"Trace_{i+1}"
            
            # Create UseCase containing this sequence
            usecase, usecase_id = self.xmi_writer.create_packaged_element(
                usecases_pkg, "UseCase", trace_name
            )
            
            # Create Interaction inside the UseCase
            interaction = ET.SubElement(usecase, "ownedBehavior")
            interaction.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}type", "uml:Interaction")
            interaction_id = self.xmi_writer.generate_uuid()
            interaction.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}id", interaction_id)
            interaction.set("name", f"{trace_name}_Interaction")
            
            # Track interaction ID for MARTE GaAnalysisContext
            self.interaction_ids[trace_name] = interaction_id
            
            # Get services for this trace
            services = trace.get_all_service_names()
            
            # Create lifelines (without 'represents' to avoid Papyrus IllegalValueException)
            lifeline_ids: Dict[str, str] = {}
            for service in sorted(services):
                lifeline = ET.SubElement(interaction, "lifeline")
                lifeline_id = self.xmi_writer.generate_uuid()
                lifeline.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}id", lifeline_id)
                lifeline.set("name", service)
                
                # Note: We intentionally don't set 'represents' attribute
                # as it causes IllegalValueException in Papyrus when referencing
                # components from a different package. The lifeline name is 
                # sufficient to identify the corresponding component.
                
                lifeline_ids[service] = lifeline_id
            
            # Create messages from spans
            sorted_spans = trace.get_spans_sorted_by_time()
            span_to_service: Dict[str, str] = {}
            
            for span in sorted_spans:
                span_to_service[span.span_id] = trace.get_service_name(span)
            
            msg_counter = 0
            for span in sorted_spans:
                current_service = trace.get_service_name(span)
                parent_span_id = span.get_parent_span_id()
                
                if parent_span_id:
                    parent_service = span_to_service.get(parent_span_id)
                    
                    if parent_service and parent_service != current_service:
                        from_lifeline = lifeline_ids.get(parent_service)
                        to_lifeline = lifeline_ids.get(current_service)
                        
                        if from_lifeline and to_lifeline:
                            # Create send/receive events
                            send_event = ET.SubElement(interaction, "fragment")
                            send_event.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}type", "uml:MessageOccurrenceSpecification")
                            send_id = self.xmi_writer.generate_uuid()
                            send_event.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}id", send_id)
                            send_event.set("name", f"msg{msg_counter}_send")
                            send_event.set("covered", from_lifeline)
                            
                            recv_event = ET.SubElement(interaction, "fragment")
                            recv_event.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}type", "uml:MessageOccurrenceSpecification")
                            recv_id = self.xmi_writer.generate_uuid()
                            recv_event.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}id", recv_id)
                            recv_event.set("name", f"msg{msg_counter}_recv")
                            recv_event.set("covered", to_lifeline)
                            
                            # Create message
                            clean_op = extract_simple_operation_name(span.operation_name)
                            message = ET.SubElement(interaction, "message")
                            message_id = self.xmi_writer.generate_uuid()
                            message.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}id", message_id)
                            message.set("name", clean_op)
                            message.set("messageSort", "synchCall")
                            message.set("sendEvent", send_id)
                            message.set("receiveEvent", recv_id)
                            
                            # Reference the operation on the target component
                            target_ops = self.operation_ids.get(current_service, {})
                            if clean_op in target_ops:
                                message.set("signature", target_ops[clean_op])
                            
                            # Track message ID and duration for MARTE PaStep
                            # Duration is in microseconds, convert to milliseconds
                            duration_ms = span.duration / 1000.0
                            is_async = span.get_tag('span.kind') == 'producer'
                            self.message_ids.append((message_id, duration_ms, is_async))
                            
                            msg_counter += 1
        
        logger.info(f"Generated {len(traces)} sequence(s) inside Use Cases")
    
    def _apply_marte_stereotypes(self, root: ET.Element):
        """
        Apply MARTE stereotypes to all collected elements.
        
        This method adds stereotype applications at the XMI root level,
        after all UML elements have been created.
        """
        # Apply <<RtUnit>> to all components
        for service, component_id in self.component_ids.items():
            self.marte_writer.apply_rt_unit(
                root, 
                component_id,
                is_active=True
            )
        
        # Apply <<GaExecHost>> to all nodes
        for node_name, node_id in self.node_ids.items():
            self.marte_writer.apply_ga_exec_host(
                root,
                node_id,
                speed_factor=1.0
            )
        
        # Apply <<GaAnalysisContext>> to all interactions
        for trace_name, interaction_id in self.interaction_ids.items():
            self.marte_writer.apply_ga_analysis_context(
                root,
                interaction_id,
                context_params={'isSingleMode': True}
            )
        
        # Apply <<PaStep>> to all messages with timing
        for message_id, duration_ms, is_async in self.message_ids:
            self.marte_writer.apply_pa_step(
                root,
                message_id,
                host_demand_ms=duration_ms,
                prob=1.0,
                no_sync=is_async
            )
    
    def _extract_node(self, metadata: Dict[str, any]) -> str:
        """Extract node name from service metadata."""
        if not metadata:
            return "Node-Unknown"
        
        # Try various metadata keys
        for key in ['hostname', 'host.name', 'node.name', 'k8s.pod.name', 'pod.name']:
            value = metadata.get(key)
            if value:
                return extract_base_name(str(value))
        
        return f"Node-{abs(hash(str(metadata)) % 10000)}"
