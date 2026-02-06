"""Utility functions for name cleaning and formatting."""

import re


def clean_operation_name(operation_name: str) -> str:
    """
    Clean an operation name for use in UML diagrams.
    
    Args:
        operation_name: The raw operation name
        
    Returns:
        Cleaned operation name
    """
    if not operation_name:
        return "unknown"
    
    # Remove HTTP method prefixes
    cleaned = re.sub(r'^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\s+', '', operation_name)
    
    # Replace special characters with underscores
    cleaned = re.sub(r'[^\w\-/.]', '_', cleaned)
    
    # Remove leading/trailing underscores
    cleaned = cleaned.strip('_')
    
    return cleaned if cleaned else "unknown"


def clean_trace_name(name: str) -> str:
    """
    Clean trace name by removing common prefixes.
    
    Args:
        name: Original trace name
        
    Returns:
        Cleaned trace name
    """
    if not name:
        return name
    
    # Remove common prefixes (case insensitive)
    prefixes = [
        ('traccia_', 8),
        ('traccia-', 8),
        ('taccia_', 7),
        ('taccia-', 7),
        ('trace_', 6),
        ('trace-', 6),
    ]
    
    name_lower = name.lower()
    for prefix, length in prefixes:
        if name_lower.startswith(prefix):
            cleaned = name[length:]
            return cleaned if cleaned else name
    
    return name


def extract_base_name(name: str) -> str:
    """
    Extract base name by removing deployment hash suffixes.
    Works with Kubernetes pod names, Docker container names.
    
    Examples:
        - "recommendationservice-7d5c8f9b8-xk7pt" -> "recommendationservice"
        - "frontend-abc123def456" -> "frontend"
    
    Args:
        name: Name with potential hash suffix
        
    Returns:
        Base name without hash
    """
    if not name:
        return name
    
    # Remove hash suffixes (Kubernetes pattern: -[hash]-[random])
    cleaned = re.sub(r'-[a-f0-9]{8,}(-[a-z0-9]{5})?$', '', name)
    
    return cleaned


def extract_simple_operation_name(operation_name: str) -> str:
    """
    Extract only the simple operation name from a full path.
    
    Examples:
        - "hipstershop.CartService/EmptyCart" -> "EmptyCart"
        - "/hipstershop.EmailService/SendOrderConfirmation" -> "SendOrderConfirmation"
        - "grpc.hipstershop.CurrencyService/Convert" -> "Convert"
        - "GET /api/users" -> "users"
        - "frontend" -> "frontend"
    
    Args:
        operation_name: The full operation name with path
        
    Returns:
        Simple operation name (final part only)
    """
    if not operation_name:
        return "unknown"
    
    # First clean the operation name
    cleaned = clean_operation_name(operation_name)
    
    # Extract part after the last "/" if present
    if '/' in cleaned:
        cleaned = cleaned.rsplit('/', 1)[-1]
    
    # If still has dots (like "service.method"), take the last part
    if '.' in cleaned:
        cleaned = cleaned.rsplit('.', 1)[-1]
    
    return cleaned if cleaned else "unknown"


def sanitize_xml_name(name: str) -> str:
    """
    Sanitize a name for use in XML/XMI.
    
    Args:
        name: Original name
        
    Returns:
        XML-safe name
    """
    if not name:
        return "unnamed"
    
    # Replace invalid XML characters
    sanitized = re.sub(r'[<>&"\']', '_', name)
    
    # Ensure it doesn't start with a number or special char
    if sanitized and not sanitized[0].isalpha():
        sanitized = 'n_' + sanitized
    
    return sanitized
