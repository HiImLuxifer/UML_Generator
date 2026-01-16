"""Jaeger API client for fetching traces."""

import logging
from typing import List, Optional
from datetime import datetime, timedelta
from .trace_reader import TraceReader
from ..models import Trace

try:
    import requests
except ImportError:
    requests = None


logger = logging.getLogger(__name__)


class JaegerApiClient(TraceReader):
    """Client for fetching traces from Jaeger API."""
    
    def __init__(self, jaeger_url: str, service_name: Optional[str] = None, 
                 lookback: Optional[str] = None, limit: int = 100):
        """
        Initialize the Jaeger API client.
        
        Args:
            jaeger_url: Base URL of Jaeger (e.g., http://localhost:16686)
            service_name: Service name to filter traces
            lookback: Lookback time (e.g., '24h', '1d')
            limit: Maximum number of traces to fetch
        """
        if requests is None:
            raise ImportError("requests library is required for Jaeger API client. "
                            "Install it with: pip install requests")
        
        self.jaeger_url = jaeger_url.rstrip('/')
        self.service_name = service_name
        self.lookback = lookback or '24h'
        self.limit = limit
    
    def read_traces(self) -> List[Trace]:
        """
        Fetch traces from Jaeger API.
        
        Returns:
            List of Trace objects
        """
        logger.info(f"Fetching traces from Jaeger: {self.jaeger_url}")
        
        # Build API endpoint
        api_url = f"{self.jaeger_url}/api/traces"
        
        # Build query parameters
        params = {
            'limit': self.limit,
            'lookback': self.lookback
        }
        
        if self.service_name:
            params['service'] = self.service_name
            logger.info(f"Filtering by service: {self.service_name}")
        
        try:
            response = requests.get(api_url, params=params, timeout=30)
            response.raise_for_status()
            
            data = response.json()
            
            # Parse traces
            traces = []
            if 'data' in data and isinstance(data['data'], list):
                for trace_data in data['data']:
                    traces.append(Trace.from_dict(trace_data))
            
            logger.info(f"Fetched {len(traces)} trace(s) from Jaeger API")
            return traces
            
        except requests.exceptions.RequestException as e:
            logger.error(f"Error fetching traces from Jaeger API: {e}")
            raise Exception(f"Failed to fetch traces from Jaeger: {e}")
