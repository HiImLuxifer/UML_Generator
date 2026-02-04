"""
MARTE Profile support for UML XMI generation.

This module provides functions to apply MARTE (Modeling and Analysis of 
Real-Time and Embedded systems) stereotypes and tagged values to UML elements.

MARTE Profile: OMG formal/19-04-01 (Version 1.2)

Stereotypes implemented:
- GaAnalysisContext: Applied to Interactions for performance analysis context
- PaStep: Applied to Messages with timing information (hostDemand, prob, etc.)
- GaExecHost: Applied to Nodes representing execution hosts
- RtUnit: Applied to Components representing real-time units
"""

import xml.etree.ElementTree as ET
from typing import Dict, Optional, List
import uuid


# MARTE Profile namespace
MARTE_NS = "http://www.omg.org/spec/MARTE/1.2"
MARTE_PREFIX = "MARTE_AnalysisModel"

# Sub-profiles
GQAM_NS = f"{MARTE_NS}/GQAM"  # Generic Quantitative Analysis Modeling
PAM_NS = f"{MARTE_NS}/PAM"    # Performance Analysis Modeling
GRM_NS = f"{MARTE_NS}/GRM"    # Generic Resource Modeling

# Papyrus MARTE pathmap
PAPYRUS_MARTE_PATHMAP = "pathmap://Papyrus_MARTE_modelLibrary/MARTE_PrimitiveTypes.library.uml"
PAPYRUS_MARTE_PROFILE = "pathmap://MARTE_PROFILE/MARTE.profile.uml"


class MarteProfileWriter:
    """
    Helper class for writing MARTE profile applications to XMI.
    
    This class manages the creation of MARTE stereotype applications
    that are added at the end of the XMI document, after the UML Model.
    """
    
    def __init__(self, xmi_namespace: str):
        """
        Initialize MARTE profile writer.
        
        Args:
            xmi_namespace: XMI namespace to use (e.g., http://www.omg.org/spec/XMI/20131001)
        """
        self.xmi_ns = xmi_namespace
        self.stereotype_applications: List[ET.Element] = []
        
        # Register MARTE namespace
        ET.register_namespace('MARTE_GQAM', GQAM_NS)
        ET.register_namespace('MARTE_PAM', PAM_NS)
        ET.register_namespace('MARTE_GRM', GRM_NS)
    
    @staticmethod
    def generate_uuid() -> str:
        """Generate a unique UUID."""
        return f"_marte_{uuid.uuid4()}"
    
    def add_profile_application(self, model: ET.Element) -> ET.Element:
        """
        Add MARTE profile application to the UML Model.
        
        This creates the profileApplication element that references the
        MARTE profile, enabling stereotype usage.
        
        Args:
            model: The UML Model element
            
        Returns:
            The profileApplication element
        """
        profile_app = ET.SubElement(model, "profileApplication")
        profile_app.set(f"{{{self.xmi_ns}}}type", "uml:ProfileApplication")
        profile_app.set(f"{{{self.xmi_ns}}}id", self.generate_uuid())
        
        # Reference to applied profile (MARTE)
        applied_profile = ET.SubElement(profile_app, "appliedProfile")
        applied_profile.set(f"{{{self.xmi_ns}}}type", "uml:Profile")
        applied_profile.set("href", f"{PAPYRUS_MARTE_PROFILE}#_ar8OsAPMEdyuUvV5MHMtrQ")
        
        return profile_app
    
    def apply_ga_analysis_context(self, root: ET.Element, 
                                   interaction_id: str,
                                   context_params: Optional[Dict] = None) -> ET.Element:
        """
        Apply <<GaAnalysisContext>> stereotype to an Interaction.
        
        GaAnalysisContext defines the context for quantitative analysis,
        typically applied to sequence diagrams (Interactions).
        
        Args:
            root: Root XMI element (stereotype applications go here)
            interaction_id: ID of the Interaction element
            context_params: Optional context parameters
            
        Returns:
            The stereotype application element
        """
        stereotype = ET.SubElement(root, f"{{{GQAM_NS}}}GaAnalysisContext")
        stereotype.set(f"{{{self.xmi_ns}}}id", self.generate_uuid())
        stereotype.set("base_NamedElement", interaction_id)
        
        # Add context parameters if provided
        if context_params:
            if 'isSingleMode' in context_params:
                stereotype.set("isSingleMode", str(context_params['isSingleMode']).lower())
        
        return stereotype
    
    def apply_pa_step(self, root: ET.Element,
                      message_id: str,
                      host_demand_ms: float,
                      prob: float = 1.0,
                      no_sync: bool = False,
                      resp_t_ms: Optional[float] = None) -> ET.Element:
        """
        Apply <<PaStep>> stereotype to a Message.
        
        PaStep (Performance Analysis Step) captures performance attributes
        of execution steps, such as processing time and probability.
        
        Args:
            root: Root XMI element
            message_id: ID of the Message element
            host_demand_ms: Host demand (execution time) in milliseconds
            prob: Probability of execution (0.0-1.0), default 1.0
            no_sync: Whether this is an asynchronous call
            resp_t_ms: Response time in milliseconds (optional)
            
        Returns:
            The stereotype application element
        """
        stereotype = ET.SubElement(root, f"{{{PAM_NS}}}PaStep")
        stereotype.set(f"{{{self.xmi_ns}}}id", self.generate_uuid())
        stereotype.set("base_NamedElement", message_id)
        
        # Host demand - the primary timing value from Jaeger spans
        # Format: (value, unit) as VSL expression
        stereotype.set("hostDemand", f"(value={host_demand_ms:.3f},unit=ms)")
        
        # Probability (default 1.0 = always executed)
        if prob != 1.0:
            stereotype.set("prob", f"{prob:.4f}")
        
        # Synchronization mode
        if no_sync:
            stereotype.set("noSync", "true")
        
        # Response time if provided
        if resp_t_ms is not None:
            stereotype.set("respT", f"(value={resp_t_ms:.3f},unit=ms)")
        
        return stereotype
    
    def apply_ga_exec_host(self, root: ET.Element,
                           node_id: str,
                           speed_factor: float = 1.0,
                           comm_tx_ovh: Optional[float] = None,
                           comm_rcv_ovh: Optional[float] = None) -> ET.Element:
        """
        Apply <<GaExecHost>> stereotype to a Node.
        
        GaExecHost represents an execution host resource with
        processing capabilities and communication overhead.
        
        Args:
            root: Root XMI element
            node_id: ID of the Node element
            speed_factor: Relative processor speed (default 1.0)
            comm_tx_ovh: Communication transmit overhead (ms)
            comm_rcv_ovh: Communication receive overhead (ms)
            
        Returns:
            The stereotype application element
        """
        stereotype = ET.SubElement(root, f"{{{GRM_NS}}}GaExecHost")
        stereotype.set(f"{{{self.xmi_ns}}}id", self.generate_uuid())
        stereotype.set("base_Classifier", node_id)
        
        # Speed factor (processing speed relative to reference)
        if speed_factor != 1.0:
            stereotype.set("speedFactor", f"{speed_factor:.2f}")
        
        # Communication overhead values
        if comm_tx_ovh is not None:
            stereotype.set("commTxOvh", f"(value={comm_tx_ovh:.3f},unit=ms)")
        if comm_rcv_ovh is not None:
            stereotype.set("commRcvOvh", f"(value={comm_rcv_ovh:.3f},unit=ms)")
        
        return stereotype
    
    def apply_rt_unit(self, root: ET.Element,
                      component_id: str,
                      is_periodic: bool = False,
                      period_ms: Optional[float] = None,
                      is_active: bool = True) -> ET.Element:
        """
        Apply <<RtUnit>> stereotype to a Component.
        
        RtUnit represents a real-time unit (active or passive component)
        with optional periodic behavior.
        
        Args:
            root: Root XMI element
            component_id: ID of the Component element
            is_periodic: Whether the component has periodic behavior
            period_ms: Period in milliseconds (if periodic)
            is_active: Whether this is an active component
            
        Returns:
            The stereotype application element
        """
        stereotype = ET.SubElement(root, f"{{{GRM_NS}}}RtUnit")
        stereotype.set(f"{{{self.xmi_ns}}}id", self.generate_uuid())
        stereotype.set("base_BehavioredClassifier", component_id)
        
        # Active/passive
        stereotype.set("isActive", str(is_active).lower())
        
        # Periodic behavior
        if is_periodic:
            stereotype.set("isPeriodic", "true")
            if period_ms is not None:
                stereotype.set("period", f"(value={period_ms:.3f},unit=ms)")
        
        return stereotype
    
    def apply_ga_workload_event(self, root: ET.Element,
                                 element_id: str,
                                 pattern: str = "closed",
                                 population: int = 1) -> ET.Element:
        """
        Apply <<GaWorkloadEvent>> stereotype for workload modeling.
        
        Args:
            root: Root XMI element
            element_id: ID of the element to annotate
            pattern: Workload pattern ('closed' or 'open')
            population: Number of concurrent workload instances
            
        Returns:
            The stereotype application element
        """
        stereotype = ET.SubElement(root, f"{{{GQAM_NS}}}GaWorkloadEvent")
        stereotype.set(f"{{{self.xmi_ns}}}id", self.generate_uuid())
        stereotype.set("base_NamedElement", element_id)
        stereotype.set("pattern", pattern)
        
        if pattern == "closed":
            stereotype.set("population", str(population))
        
        return stereotype
