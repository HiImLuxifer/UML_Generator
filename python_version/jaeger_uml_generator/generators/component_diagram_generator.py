"""Component diagram generator with MagicDraw-style output."""

import logging
import xml.etree.ElementTree as ET
from typing import List, Dict, Set
from .diagram_generator import DiagramGenerator
from ..models import Trace
from ..analyzer import TraceAggregator
from ..renderer import XmiWriter, XmiFormat
from ..utils import clean_operation_name


logger = logging.getLogger(__name__)


class ComponentDiagramGenerator(DiagramGenerator):
    """Generates UML Component Diagrams in XMI 2.5.1 format from aggregated Jaeger traces.
    
    This generator produces MagicDraw-style component diagrams with:
    - Package organization for components (Services, Database, etc.)
    - Proper Interface elements with operations
    - Usage dependencies between components and interfaces
    - InterfaceRealization for provided interfaces
    """
    
    # Service categories for package organization
    SERVICE_CATEGORIES = {
        'frontend': 'Presentation',
        'ui': 'Presentation',
        'web': 'Presentation',
        'client': 'Presentation',
        'gateway': 'Presentation',
        'database': 'DataLayer',
        'db': 'DataLayer',
        'mongo': 'DataLayer',
        'postgres': 'DataLayer',
        'mysql': 'DataLayer',
        'redis': 'DataLayer',
        'cache': 'DataLayer',
        'memcache': 'DataLayer',
    }
    
    def __init__(self, xmi_format: str = "papyrus"):
        """Initialize generator with XMI format.
        
        Args:
            xmi_format: Output format ('papyrus' or 'magicdraw')
        """
        format_enum = XmiFormat(xmi_format)
        self.xmi_writer = XmiWriter(format_enum)
    
    def get_diagram_type(self) -> str:
        return "component"
    
    def generate_xmi(self, traces: List[Trace]) -> str:
        """
        Generate XMI for component diagram from multiple traces.
        
        Args:
            traces: List of Trace objects
            
        Returns:
            XMI content as string
        """
        if not traces:
            logger.warning("No traces provided for component diagram generation")
            return ""
        
        try:
            aggregator = TraceAggregator(traces)
            
            # Determine model name
            model_name = "ComponentDiagram"
            if traces and traces[0].source_name:
                model_name = f"{traces[0].source_name}_Component"
            
            # Create XMI document
            root = self.xmi_writer.create_xmi_document(model_name)
            model = self.xmi_writer.get_model_element(root)
            
            # Get all services and their relationships
            services = aggregator.get_all_services()
            service_metadata = aggregator.get_service_metadata()
            service_calls = aggregator.get_service_calls()
            service_operations = aggregator.get_service_operations()
            
            # Organize services into packages by category
            categorized_services = self._categorize_services(services)
            
            # Track created elements to reference them
            component_ids: Dict[str, str] = {}
            interface_ids: Dict[str, str] = {}
            package_ids: Dict[str, str] = {}
            
            # Create packages and components
            for category, category_services in categorized_services.items():
                if category_services:
                    # Create package for this category
                    package_elem, package_id = self.xmi_writer.create_package(model, category)
                    package_ids[category] = package_id
                    
                    # Create components inside the package
                    for service in category_services:
                        metadata = service_metadata.get(service, {})
                        component_id = self._create_component(package_elem, service, metadata)
                        component_ids[service] = component_id
            
            # Create interfaces package
            interfaces_package, _ = self.xmi_writer.create_package(model, "Interfaces")
            
            # Create interfaces for services that provide operations
            for callee_service in services:
                # Get operations that this service provides
                provided_ops = self._get_provided_operations(callee_service, service_calls)
                
                if provided_ops:
                    interface_name = f"I{self._capitalize_service_name(callee_service)}"
                    
                    # Create interface with operations (limit to 15 for readability)
                    ops_list = list(provided_ops)[:15]
                    clean_ops = [clean_operation_name(op) for op in ops_list]
                    
                    interface_elem, interface_id = self.xmi_writer.create_interface(
                        interfaces_package, interface_name, clean_ops
                    )
                    interface_ids[callee_service] = interface_id
                    
                    # Add comment if more operations exist
                    if len(provided_ops) > 15:
                        self.xmi_writer.add_comment(
                            interface_elem,
                            f"+{len(provided_ops) - 15} more operations"
                        )
            
            # Create relationships package
            relationships_package, _ = self.xmi_writer.create_package(model, "Relationships")
            
            # Create Usage dependencies and InterfaceRealizations
            total_usages = 0
            total_realizations = 0
            
            for caller_service, callee_map in service_calls.items():
                for callee_service, operations in callee_map.items():
                    caller_id = component_ids.get(caller_service)
                    callee_id = component_ids.get(callee_service)
                    interface_id = interface_ids.get(callee_service)
                    
                    if caller_id and interface_id:
                        # Create Usage dependency (caller uses interface)
                        usage_name = f"{caller_service}_uses_{callee_service}"
                        self.xmi_writer.create_usage(
                            relationships_package,
                            usage_name,
                            caller_id,
                            interface_id
                        )
                        total_usages += 1
                    
                    # Create InterfaceRealization (callee provides interface)
                    if callee_id and interface_id:
                        realization_name = f"{callee_service}_provides_I{callee_service}"
                        # Only create if not already created
                        if callee_service not in self._created_realizations:
                            self.xmi_writer.create_interface_realization(
                                relationships_package,
                                realization_name,
                                callee_id,
                                interface_id
                            )
                            self._created_realizations.add(callee_service)
                            total_realizations += 1
            
            logger.info(f"Generated component diagram XMI with {len(services)} service(s), "
                       f"{len(interface_ids)} interface(s), {total_usages} usage(s), "
                       f"and {total_realizations} realization(s)")
            
            return self.xmi_writer.document_to_string(root)
            
        except Exception as e:
            logger.error(f"Failed to generate component diagram XMI: {e}")
            import traceback
            traceback.print_exc()
            return ""
    
    # Track created realizations to avoid duplicates
    _created_realizations: Set[str] = set()
    
    def _categorize_services(self, services: Set[str]) -> Dict[str, Set[str]]:
        """
        Categorize services into packages based on naming conventions.
        
        Args:
            services: Set of service names
            
        Returns:
            Dictionary mapping category name to set of services
        """
        categories: Dict[str, Set[str]] = {
            'Presentation': set(),
            'Services': set(),
            'DataLayer': set()
        }
        
        for service in services:
            lower_name = service.lower()
            categorized = False
            
            for keyword, category in self.SERVICE_CATEGORIES.items():
                if keyword in lower_name:
                    categories[category].add(service)
                    categorized = True
                    break
            
            if not categorized:
                categories['Services'].add(service)
        
        # Remove empty categories
        return {k: v for k, v in categories.items() if v}
    
    def _create_component(self, parent: ET.Element, service_name: str, 
                         metadata: Dict[str, any]) -> str:
        """
        Create a UML Component element.
        
        Args:
            parent: Parent element (package)
            service_name: Service name
            metadata: Service metadata
            
        Returns:
            Component ID
        """
        # Detect stereotype
        stereotype = self._detect_service_stereotype(service_name, metadata)
        
        # Create component using the new method
        component, component_id = self.xmi_writer.create_packaged_element(
            parent, "Component", service_name, visibility="public"
        )
        
        # Add stereotype as comment
        if stereotype:
            self.xmi_writer.add_comment(component, f"«{stereotype}»")
        
        return component_id
    
    def _get_provided_operations(self, service_name: str, 
                                 service_calls: Dict[str, Dict[str, Set[str]]]) -> Set[str]:
        """
        Get all operations that a service provides (is called by others).
        
        Args:
            service_name: Service name to check
            service_calls: Service calls mapping
            
        Returns:
            Set of operation names that this service provides
        """
        provided_ops = set()
        
        for caller, callees in service_calls.items():
            if service_name in callees:
                provided_ops.update(callees[service_name])
        
        return provided_ops
    
    def _capitalize_service_name(self, name: str) -> str:
        """Convert service name to PascalCase for interface naming."""
        # Remove common suffixes for cleaner names
        clean_name = name.replace('service', '').replace('Service', '')
        clean_name = clean_name.replace('-', ' ').replace('_', ' ')
        
        # Capitalize each word
        parts = clean_name.split()
        if parts:
            return ''.join(word.capitalize() for word in parts)
        
        return name.capitalize()
    
    def _detect_service_stereotype(self, service_name: str, 
                                   metadata: Dict[str, any]) -> str:
        """
        Detect the stereotype for a service based on its name and metadata.
        
        Args:
            service_name: Service name
            metadata: Service metadata
            
        Returns:
            Stereotype name or empty string
        """
        if not service_name:
            return ""
        
        lower_name = service_name.lower()
        
        # Check for frontend/UI services
        if any(keyword in lower_name for keyword in ['frontend', 'ui', 'web', 'client']):
            return "WebUI"
        
        # Check for gateway services
        if any(keyword in lower_name for keyword in ['gateway', 'api-gateway', 'ingress']):
            return "Gateway"
        
        # Check for database services
        if any(keyword in lower_name for keyword in 
               ['database', 'db', 'mongo', 'postgres', 'mysql']):
            return "Database"
        
        # Check for cache services
        if any(keyword in lower_name for keyword in ['cache', 'redis', 'memcache']):
            return "Cache"
        
        # Check metadata for gRPC
        if metadata:
            rpc_system = metadata.get('rpc.system', '')
            if isinstance(rpc_system, str) and 'grpc' in rpc_system.lower():
                return "gRPC Service"
        
        # Generic microservice for services ending in 'service'
        if lower_name.endswith('service'):
            return "Microservice"
        
        return "Component"
