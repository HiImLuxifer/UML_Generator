"""Process model representing a service."""

from dataclasses import dataclass, field
from typing import Dict, Any


@dataclass
class Process:
    """Represents a service/process in Jaeger."""
    
    service_name: str
    tags: Dict[str, Any] = field(default_factory=dict)
    
    @classmethod
    def from_dict(cls, data: dict) -> 'Process':
        """Create a Process from a dictionary."""
        service_name = data.get('serviceName', 'unknown')
        
        # Parse tags
        tags = {}
        if 'tags' in data and isinstance(data['tags'], list):
            for tag in data['tags']:
                if isinstance(tag, dict) and 'key' in tag:
                    tags[tag['key']] = tag.get('value')
        
        return cls(service_name=service_name, tags=tags)
    
    def get_tag(self, key: str, default: Any = None) -> Any:
        """Get a tag value by key."""
        return self.tags.get(key, default)
