"""Span reference model."""

from dataclasses import dataclass
from typing import Optional


@dataclass
class Reference:
    """Represents a reference between spans (parent-child relationships)."""
    
    ref_type: str  # 'CHILD_OF' or 'FOLLOWS_FROM'
    trace_id: str
    span_id: str
    
    @classmethod
    def from_dict(cls, data: dict) -> 'Reference':
        """Create a Reference from a dictionary."""
        return cls(
            ref_type=data.get('refType', ''),
            trace_id=data.get('traceID', ''),
            span_id=data.get('spanID', '')
        )
