"""Trace aggregator for analyzing multiple traces."""

import logging
from typing import List, Set, Dict
from ..models import Trace, Span


logger = logging.getLogger(__name__)


class TraceAggregator:
    """Aggregates and analyzes data from multiple traces."""
    
    def __init__(self, traces: List[Trace]):
        """
        Initialize the aggregator with a list of traces.
        
        Args:
            traces: List of Trace objects
        """
        self.traces = traces if traces else []
        
        # Aggregated data
        self.all_services: Set[str] = set()
        self.service_operations: Dict[str, Set[str]] = {}
        self.service_dependencies: Dict[str, Set[str]] = {}
        self.service_metadata: Dict[str, Dict[str, any]] = {}
        # Map: fromService -> toService -> Set of operations called
        self.service_calls: Dict[str, Dict[str, Set[str]]] = {}
        
        # Analyze all traces
        self._analyze()
    
    def _analyze(self):
        """Analyze all traces to extract aggregated information."""
        logger.info(f"Analyzing {len(self.traces)} trace(s)")
        
        for trace in self.traces:
            self._analyze_trace(trace)
        
        logger.info(f"Found {len(self.all_services)} unique service(s)")
        logger.info(f"Service list: {sorted(self.all_services)}")
    
    def _analyze_trace(self, trace: Trace):
        """Analyze a single trace."""
        if not trace or not trace.spans:
            return
        
        for span in trace.spans:
            service_name = trace.get_service_name(span)
            
            # Collect service
            self.all_services.add(service_name)
            
            # Collect operations
            if service_name not in self.service_operations:
                self.service_operations[service_name] = set()
            self.service_operations[service_name].add(span.operation_name)
            
            # Collect metadata from process tags
            if span.process_id:
                process = trace.get_process(span.process_id)
                if process and process.tags:
                    if service_name not in self.service_metadata:
                        self.service_metadata[service_name] = {}
                    self.service_metadata[service_name].update(process.tags)
            
            # Analyze dependencies (parent-child relationships)
            parent_span_id = span.get_parent_span_id()
            if parent_span_id:
                parent_span = trace.get_span(parent_span_id)
                if parent_span:
                    parent_service = trace.get_service_name(parent_span)
                    if service_name != parent_service:
                        # Cross-service dependency
                        if parent_service not in self.service_dependencies:
                            self.service_dependencies[parent_service] = set()
                        self.service_dependencies[parent_service].add(service_name)
                        
                        # Track specific operation calls between services
                        if parent_service not in self.service_calls:
                            self.service_calls[parent_service] = {}
                        if service_name not in self.service_calls[parent_service]:
                            self.service_calls[parent_service][service_name] = set()
                        self.service_calls[parent_service][service_name].add(span.operation_name)
    
    def get_all_services(self) -> Set[str]:
        """Get all unique service names."""
        return self.all_services.copy()
    
    def get_service_operations(self) -> Dict[str, Set[str]]:
        """Get all operations for each service."""
        return {k: v.copy() for k, v in self.service_operations.items()}
    
    def get_service_dependencies(self) -> Dict[str, Set[str]]:
        """Get dependencies between services."""
        return {k: v.copy() for k, v in self.service_dependencies.items()}
    
    def get_service_metadata(self) -> Dict[str, Dict[str, any]]:
        """Get metadata for each service."""
        return {k: v.copy() for k, v in self.service_metadata.items()}
    
    def get_operations_for_service(self, service_name: str) -> Set[str]:
        """Get all operations for a specific service."""
        return self.service_operations.get(service_name, set()).copy()
    
    def get_dependencies_for_service(self, service_name: str) -> Set[str]:
        """Get all services that this service depends on."""
        return self.service_dependencies.get(service_name, set()).copy()
    
    def get_metadata_for_service(self, service_name: str) -> Dict[str, any]:
        """Get metadata for a specific service."""
        return self.service_metadata.get(service_name, {}).copy()
    
    def get_service_calls(self) -> Dict[str, Dict[str, Set[str]]]:
        """
        Get all service calls with specific operations.
        Returns: fromService -> toService -> Set of operations called
        """
        return {
            from_svc: {to_svc: ops.copy() for to_svc, ops in targets.items()}
            for from_svc, targets in self.service_calls.items()
        }
