"""Abstract base class for trace readers."""

from abc import ABC, abstractmethod
from typing import List
from ..models import Trace


class TraceReader(ABC):
    """Abstract base class for reading Jaeger traces from different sources."""
    
    @abstractmethod
    def read_traces(self) -> List[Trace]:
        """
        Read traces from the configured source.
        
        Returns:
            List of Trace objects
        """
        pass
