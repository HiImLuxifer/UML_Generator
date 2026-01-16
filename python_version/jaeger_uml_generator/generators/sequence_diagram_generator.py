"""Sequence diagram generator."""

import logging
import xml.etree.ElementTree as ET
from typing import List, Dict
from .diagram_generator import DiagramGenerator
from ..models import Trace, Span
from ..renderer import XmiWriter, XmiFormat
from ..utils import clean_operation_name


logger = logging.getLogger(__name__)


class SequenceDiagramGenerator(DiagramGenerator):
    """Generates UML Sequence Diagrams in XMI 2.5.1 format from Jaeger traces."""
    
    def __init__(self, xmi_format: str = "papyrus"):
        """Initialize generator with XMI format.
        
        Args:
            xmi_format: Output format ('papyrus' or 'magicdraw')
        """
        format_enum = XmiFormat(xmi_format)
        self.xmi_writer = XmiWriter(format_enum)
    
    def get_diagram_type(self) -> str:
        return "sequence"
    
    def generate_xmi(self, traces: List[Trace]) -> str:
        """
        Generate XMI for the first trace (sequence diagrams are per-trace).
        
        Args:
            traces: List of Trace objects
            
        Returns:
            XMI content as string
        """
        if not traces:
            logger.warning("No traces provided for sequence diagram generation")
            return ""
        
        # For sequence diagrams, generate for the first trace
        return self.generate_xmi_for_trace(traces[0], 0)
    
    def generate_xmi_for_trace(self, trace: Trace, index: int) -> str:
        """
        Generate XMI for a single trace.
        
        Args:
            trace: Trace object
            index: Trace index (for naming)
            
        Returns:
            XMI content as string
        """
        if not trace:
            logger.warning("Null trace provided for sequence diagram generation")
            return ""
        
        try:
            # Determine model name
            model_name = "SequenceDiagram"
            if trace.source_name:
                model_name = f"{trace.source_name}_Sequence"
            
            # Create XMI document
            root = self.xmi_writer.create_xmi_document(model_name)
            model = self.xmi_writer.get_model_element(root)
            
            # Get all services
            services = trace.get_all_service_names()
            
            # First, create Class elements for each service as types for the lifelines
            service_class_ids: Dict[str, str] = {}
            for service in services:
                class_elem, class_id = self.xmi_writer.create_packaged_element(
                    model, "Class", service
                )
                service_class_ids[service] = class_id
            
            # Create Collaboration to hold the participants
            collaboration, collab_id = self.xmi_writer.create_packaged_element(
                model, "Collaboration", f"{model_name}_Collaboration"
            )
            
            # Create ownedAttributes (Properties) in the Collaboration for each service
            service_property_ids: Dict[str, str] = {}
            for service in services:
                class_id = service_class_ids[service]
                prop_elem, prop_id = self.xmi_writer.create_owned_element(
                    collaboration, "ownedAttribute",
                    name=service, type=class_id
                )
                service_property_ids[service] = prop_id
            
            # Create Interaction element
            interaction, interaction_id = self.xmi_writer.create_packaged_element(
                model, "Interaction", f"{model_name}_Interaction"
            )
            
            # Create lifelines referencing the properties
            service_to_lifeline_id: Dict[str, str] = {}
            
            for service in services:
                property_id = service_property_ids[service]
                lifeline_id = self._create_lifeline(interaction, service, property_id)
                service_to_lifeline_id[service] = lifeline_id
            
            # Process spans and create messages
            sorted_spans = trace.get_spans_sorted_by_time()
            span_to_service: Dict[str, str] = {}
            
            for span in sorted_spans:
                span_to_service[span.span_id] = trace.get_service_name(span)
            
            message_counter = 0
            total_messages = 0
            
            for span in sorted_spans:
                current_service = trace.get_service_name(span)
                parent_span_id = span.get_parent_span_id()
                
                if parent_span_id:
                    parent_service = span_to_service.get(parent_span_id)
                    
                    if parent_service and parent_service != current_service:
                        # Cross-service call
                        from_lifeline_id = service_to_lifeline_id.get(parent_service)
                        to_lifeline_id = service_to_lifeline_id.get(current_service)
                        
                        if from_lifeline_id and to_lifeline_id:
                            clean_operation = clean_operation_name(span.operation_name)
                            
                            self._create_message(
                                interaction,
                                f"msg{message_counter}",
                                clean_operation,
                                from_lifeline_id,
                                to_lifeline_id,
                                "synchCall",
                                span.duration
                            )
                            
                            message_counter += 1
                            total_messages += 1
            
            logger.info(f"Generated sequence diagram XMI for trace {trace.trace_id} "
                       f"with {len(services)} participants and {total_messages} messages")
            
            return self.xmi_writer.document_to_string(root)
            
        except Exception as e:
            logger.error(f"Failed to generate sequence diagram XMI: {e}")
            return ""
    
    def _create_lifeline(self, interaction: ET.Element, service_name: str, 
                         represents_id: str) -> str:
        """
        Create a UML Lifeline element.
        
        Args:
            interaction: Parent interaction element
            service_name: Service name
            represents_id: ID of the Property this lifeline represents
            
        Returns:
            Lifeline ID
        """
        lifeline = ET.SubElement(interaction, "lifeline")
        lifeline_id = self.xmi_writer.generate_uuid()
        lifeline.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}id", lifeline_id)
        lifeline.set("name", service_name)
        lifeline.set("represents", represents_id)
        
        return lifeline_id
    
    def _create_message(self, interaction: ET.Element, message_name: str,
                       operation_name: str, from_lifeline_id: str, to_lifeline_id: str,
                       message_sort: str, duration: int):
        """
        Create a UML Message with send and receive events.
        
        Args:
            interaction: Parent interaction element
            message_name: Message identifier
            operation_name: Operation being called
            from_lifeline_id: Source lifeline ID
            to_lifeline_id: Target lifeline ID
            message_sort: Message type (e.g., 'synchCall')
            duration: Duration in microseconds
        """
        # Create send event
        send_event = ET.SubElement(interaction, "fragment")
        send_event.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}type", "uml:MessageOccurrenceSpecification")
        send_event_id = self.xmi_writer.generate_uuid()
        send_event.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}id", send_event_id)
        send_event.set("name", f"{message_name}_send")
        send_event.set("covered", from_lifeline_id)
        
        # Create receive event
        receive_event = ET.SubElement(interaction, "fragment")
        receive_event.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}type", "uml:MessageOccurrenceSpecification")
        receive_event_id = self.xmi_writer.generate_uuid()
        receive_event.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}id", receive_event_id)
        receive_event.set("name", f"{message_name}_receive")
        receive_event.set("covered", to_lifeline_id)
        
        # Create message
        message = ET.SubElement(interaction, "message")
        message.set(f"{{{self.xmi_writer.XMI_NAMESPACE}}}id", self.xmi_writer.generate_uuid())
        message.set("name", operation_name)
        message.set("messageSort", message_sort)
        message.set("sendEvent", send_event_id)
        message.set("receiveEvent", receive_event_id)
        
        # Add timing information as a comment if available
        if duration > 0:
            self.xmi_writer.add_comment(message, f"Duration: {duration}μs")
