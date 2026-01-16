"""Trace model representing a complete distributed trace."""

from dataclasses import dataclass, field
from typing import List, Dict, Optional
from .span import Span
from .process import Process


@dataclass
class Trace:
    """Represents a complete Jaeger trace with spans and processes."""
    
    trace_id: str
    spans: List[Span] = field(default_factory=list)
    processes: Dict[str, Process] = field(default_factory=dict)
    warnings: List[str] = field(default_factory=list)
    source_name: Optional[str] = None
    
    @classmethod
    def from_dict(cls, data: dict) -> 'Trace':
        """Create a Trace from a dictionary."""
        # Parse spans
        spans = []
        if 'spans' in data and isinstance(data['spans'], list):
            spans = [Span.from_dict(span_data) for span_data in data['spans']]
        
        # Parse processes
        processes = {}
        if 'processes' in data and isinstance(data['processes'], dict):
            for process_id, process_data in data['processes'].items():
                processes[process_id] = Process.from_dict(process_data)
        
        warnings = data.get('warnings', [])
        
        return cls(
            trace_id=data.get('traceID', ''),
            spans=spans,
            processes=processes,
            warnings=warnings
        )
    
    def get_process(self, process_id: str) -> Optional[Process]:
        """Get a process by its ID."""
        return self.processes.get(process_id)
    
    def get_service_name(self, span: Span) -> str:
        """Get the service name for a given span."""
        if not span or not span.process_id:
            return 'unknown'
        
        process = self.get_process(span.process_id)
        return process.service_name if process else 'unknown'
    
    def get_span(self, span_id: str) -> Optional[Span]:
        """Get a span by its ID."""
        for span in self.spans:
            if span.span_id == span_id:
                return span
        return None
    
    def get_root_spans(self) -> List[Span]:
        """Get all root spans (spans with no parent)."""
        return [span for span in self.spans if span.is_root_span()]
    
    def get_child_spans(self, parent_span_id: str) -> List[Span]:
        """Get child spans of a given parent span."""
        children = []
        for span in self.spans:
            if span.get_parent_span_id() == parent_span_id:
                children.append(span)
        return children
    
    def get_all_service_names(self) -> List[str]:
        """Get all unique service names in this trace, sorted."""
        service_names = set()
        for span in self.spans:
            service_name = self.get_service_name(span)
            service_names.add(service_name)
        return sorted(service_names)
    
    def get_spans_sorted_by_time(self) -> List[Span]:
        """Get spans sorted by start time."""
        return sorted(self.spans, key=lambda s: s.start_time)
    
    def __str__(self) -> str:
        return f"Trace(trace_id='{self.trace_id}', spans={len(self.spans)}, processes={len(self.processes)})"
