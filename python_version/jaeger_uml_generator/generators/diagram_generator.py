"""Abstract base class for diagram generators."""

from abc import ABC, abstractmethod
from typing import List
from ..models import Trace


class DiagramGenerator(ABC):
    """Abstract base class for all diagram generators."""
    
    @abstractmethod
    def get_diagram_type(self) -> str:
        """
        Get the type of diagram this generator creates.
        
        Returns:
            Diagram type name (e.g., 'sequence', 'component', 'deployment')
        """
        pass
    
    @abstractmethod
    def generate_xmi(self, traces: List[Trace]) -> str:
        """
        Generate XMI content for the given traces.
        
        Args:
            traces: List of Trace objects
            
        Returns:
            XMI content as a string
        """
        pass
