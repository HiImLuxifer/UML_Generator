"""XMI writer for generating UML diagrams in XMI 2.5.1 format compatible with Eclipse Papyrus and MagicDraw."""

import xml.etree.ElementTree as ET
import uuid
from enum import Enum
from typing import Optional


class XmiFormat(Enum):
    """Supported XMI output formats."""
    PAPYRUS = "papyrus"      # Eclipse Papyrus format
    MAGICDRAW = "magicdraw"  # MagicDraw format


class XmiWriter:
    """Utility class for creating XMI documents compatible with Eclipse Papyrus or MagicDraw."""
    
    # Papyrus (Eclipse) namespaces
    PAPYRUS_XMI_NS = "http://www.omg.org/spec/XMI/20131001"
    PAPYRUS_UML_NS = "http://www.eclipse.org/uml2/5.0.0/UML"
    
    # MagicDraw (OMG standard) namespaces
    MAGICDRAW_XMI_NS = "http://www.omg.org/spec/XMI/20131001"
    MAGICDRAW_UML_NS = "http://www.omg.org/spec/UML/20131001"
    
    def __init__(self, format: XmiFormat = XmiFormat.PAPYRUS):
        """
        Initialize XMI writer with specified format.
        
        Args:
            format: XmiFormat enum value (PAPYRUS or MAGICDRAW)
        """
        self.format = format
        
        # Select namespaces based on format
        if format == XmiFormat.MAGICDRAW:
            self.XMI_NAMESPACE = self.MAGICDRAW_XMI_NS
            self.UML_NAMESPACE = self.MAGICDRAW_UML_NS
        else:
            self.XMI_NAMESPACE = self.PAPYRUS_XMI_NS
            self.UML_NAMESPACE = self.PAPYRUS_UML_NS
        # Register namespace prefixes to avoid ns0:, ns1: prefixes
        ET.register_namespace('xmi', self.XMI_NAMESPACE)
        ET.register_namespace('uml', self.UML_NAMESPACE)
    
    @staticmethod
    def generate_uuid() -> str:
        """Generate a unique UUID for XMI elements."""
        return str(uuid.uuid4())
    
    def create_xmi_document(self, model_name: str) -> ET.Element:
        """
        Create a new XMI document with root Model element.
        
        Args:
            model_name: Name of the UML model
            
        Returns:
            Root XMI element
        """
        # Create root XMI element with proper namespace
        root = ET.Element(f"{{{self.XMI_NAMESPACE}}}XMI")
        root.set(f"{{{self.XMI_NAMESPACE}}}version", "2.5.1")
        
        # Create Model element
        model = ET.SubElement(root, f"{{{self.UML_NAMESPACE}}}Model")
        model.set(f"{{{self.XMI_NAMESPACE}}}id", self.generate_uuid())
        model.set("name", model_name)
        
        return root
    
    def get_model_element(self, root: ET.Element) -> Optional[ET.Element]:
        """
        Get the Model element from an XMI document.
        
        Args:
            root: Root XMI element
            
        Returns:
            Model element or None
        """
        # Find the Model element
        for child in root:
            if child.tag.endswith('Model'):
                return child
        return None
    
    def create_packaged_element(self, parent: ET.Element, element_type: str, 
                                 name: str, **kwargs) -> tuple[ET.Element, str]:
        """
        Create a packagedElement with proper XMI attributes.
        
        Args:
            parent: Parent element
            element_type: UML type (e.g., 'Component', 'Interface')
            name: Element name
            **kwargs: Additional attributes
            
        Returns:
            Tuple of (element, element_id)
        """
        element = ET.SubElement(parent, "packagedElement")
        element_id = self.generate_uuid()
        element.set(f"{{{self.XMI_NAMESPACE}}}type", f"uml:{element_type}")
        element.set(f"{{{self.XMI_NAMESPACE}}}id", element_id)
        element.set("name", name)
        
        for key, value in kwargs.items():
            element.set(key, value)
        
        return element, element_id
    
    def create_owned_element(self, parent: ET.Element, element_name: str, 
                              **kwargs) -> tuple[ET.Element, str]:
        """
        Create an owned element (e.g., ownedOperation, ownedAttribute).
        
        Args:
            parent: Parent element
            element_name: Element name (e.g., 'ownedOperation')
            **kwargs: Additional attributes
            
        Returns:
            Tuple of (element, element_id)
        """
        element = ET.SubElement(parent, element_name)
        element_id = self.generate_uuid()
        element.set(f"{{{self.XMI_NAMESPACE}}}id", element_id)
        
        for key, value in kwargs.items():
            element.set(key, value)
        
        return element, element_id
    
    def document_to_string(self, root: ET.Element) -> str:
        """
        Convert XML element tree to a formatted string.
        
        Args:
            root: Root element
            
        Returns:
            Formatted XML string
        """
        # Use indent for Python 3.9+
        try:
            ET.indent(root, space="  ")
        except AttributeError:
            pass  # Python < 3.9, no indent available
        
        xml_str = ET.tostring(root, encoding='unicode', method='xml')
        
        # Add XML declaration
        return f'<?xml version="1.0" encoding="UTF-8"?>\n{xml_str}'
    
    def add_comment(self, parent: ET.Element, comment_text: str) -> ET.Element:
        """
        Add a comment element to a UML element.
        
        Args:
            parent: Parent element
            comment_text: Comment text
            
        Returns:
            Comment element
        """
        comment = ET.SubElement(parent, "ownedComment")
        comment.set(f"{{{self.XMI_NAMESPACE}}}id", self.generate_uuid())
        
        body = ET.SubElement(comment, "body")
        body.text = comment_text
        
        return comment
    
    def create_package(self, parent: ET.Element, name: str) -> tuple[ET.Element, str]:
        """
        Create a UML Package element.
        
        Args:
            parent: Parent element
            name: Package name
            
        Returns:
            Tuple of (package element, package_id)
        """
        return self.create_packaged_element(parent, "Package", name)
    
    def create_usage(self, parent: ET.Element, name: str, 
                     client_id: str, supplier_id: str) -> tuple[ET.Element, str]:
        """
        Create a UML Usage dependency element.
        Usage indicates that one element requires another for its implementation.
        
        Args:
            parent: Parent element
            name: Usage name
            client_id: ID of the client element (the one that uses)
            supplier_id: ID of the supplier element (the one being used)
            
        Returns:
            Tuple of (usage element, usage_id)
        """
        usage = ET.SubElement(parent, "packagedElement")
        usage_id = self.generate_uuid()
        usage.set(f"{{{self.XMI_NAMESPACE}}}type", "uml:Usage")
        usage.set(f"{{{self.XMI_NAMESPACE}}}id", usage_id)
        usage.set("name", name)
        
        # Add client reference
        client_elem = ET.SubElement(usage, "client")
        client_elem.set(f"{{{self.XMI_NAMESPACE}}}idref", client_id)
        
        # Add supplier reference  
        supplier_elem = ET.SubElement(usage, "supplier")
        supplier_elem.set(f"{{{self.XMI_NAMESPACE}}}idref", supplier_id)
        
        return usage, usage_id
    
    def create_interface_realization(self, parent: ET.Element, name: str,
                                     client_id: str, supplier_id: str) -> tuple[ET.Element, str]:
        """
        Create a UML InterfaceRealization element.
        
        Args:
            parent: Parent element
            name: Realization name
            client_id: ID of the implementing component
            supplier_id: ID of the interface being implemented
            
        Returns:
            Tuple of (realization element, realization_id)
        """
        realization = ET.SubElement(parent, "packagedElement")
        realization_id = self.generate_uuid()
        realization.set(f"{{{self.XMI_NAMESPACE}}}type", "uml:InterfaceRealization")
        realization.set(f"{{{self.XMI_NAMESPACE}}}id", realization_id)
        realization.set("name", name)
        
        # Add client reference (implementing component)
        client_elem = ET.SubElement(realization, "client")
        client_elem.set(f"{{{self.XMI_NAMESPACE}}}idref", client_id)
        
        # Add supplier reference (interface)
        supplier_elem = ET.SubElement(realization, "supplier")
        supplier_elem.set(f"{{{self.XMI_NAMESPACE}}}idref", supplier_id)
        
        # Add contract reference
        contract_elem = ET.SubElement(realization, "contract")
        contract_elem.set(f"{{{self.XMI_NAMESPACE}}}idref", supplier_id)
        
        return realization, realization_id
    
    def create_interface(self, parent: ET.Element, name: str,
                        operations: list = None) -> tuple[ET.Element, str]:
        """
        Create a UML Interface element with operations.
        
        Args:
            parent: Parent element
            name: Interface name
            operations: List of operation names
            
        Returns:
            Tuple of (interface element, interface_id)
        """
        interface, interface_id = self.create_packaged_element(parent, "Interface", name)
        
        if operations:
            for op_name in operations:
                self.create_owned_element(
                    interface, "ownedOperation",
                    name=op_name, visibility="public"
                )
        
        return interface, interface_id
