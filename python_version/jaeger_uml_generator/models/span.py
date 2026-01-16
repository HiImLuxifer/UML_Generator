"""Span model representing a traced operation."""

from dataclasses import dataclass, field
from typing import List, Dict, Any, Optional
from .reference import Reference


@dataclass
class Span:
    """Represents a single span in a Jaeger trace."""
    
    trace_id: str
    span_id: str
    operation_name: str
    start_time: int  # microseconds
    duration: int  # microseconds
    process_id: str
    references: List[Reference] = field(default_factory=list)
    tags: Dict[str, Any] = field(default_factory=dict)
    logs: List[Dict[str, Any]] = field(default_factory=list)
    
    @classmethod
    def from_dict(cls, data: dict) -> 'Span':
        """Create a Span from a dictionary."""
        # Parse references
        references = []
        if 'references' in data and isinstance(data['references'], list):
            references = [Reference.from_dict(ref) for ref in data['references']]
        
        # Parse tags
        tags = {}
        if 'tags' in data and isinstance(data['tags'], list):
            for tag in data['tags']:
                if isinstance(tag, dict) and 'key' in tag:
                    tags[tag['key']] = tag.get('value')
        
        # Parse logs
        logs = data.get('logs', [])
        
        return cls(
            trace_id=data.get('traceID', ''),
            span_id=data.get('spanID', ''),
            operation_name=data.get('operationName', ''),
            start_time=data.get('startTime', 0),
            duration=data.get('duration', 0),
            process_id=data.get('processID', ''),
            references=references,
            tags=tags,
            logs=logs
        )
    
    def get_parent_span_id(self) -> Optional[str]:
        """Get the parent span ID if this span has a parent."""
        for ref in self.references:
            if ref.ref_type == 'CHILD_OF':
                return ref.span_id
        return None
    
    def is_root_span(self) -> bool:
        """Check if this is a root span (no parent)."""
        return self.get_parent_span_id() is None
    
    def get_tag(self, key: str, default: Any = None) -> Any:
        """Get a tag value by key."""
        return self.tags.get(key, default)
