"""JSON file reader for Jaeger traces."""

import json
import logging
from pathlib import Path
from typing import List
from .trace_reader import TraceReader
from ..models import Trace


logger = logging.getLogger(__name__)


class JsonFileReader(TraceReader):
    """Reads Jaeger traces from JSON files or directories."""
    
    def __init__(self, path: str):
        """
        Initialize the JSON file reader.
        
        Args:
            path: Path to a JSON file or directory containing JSON files
        """
        self.path = Path(path)
    
    def read_traces(self) -> List[Trace]:
        """
        Read traces from JSON file(s).
        
        Returns:
            List of Trace objects
        """
        traces = []
        
        if self.path.is_file():
            traces.extend(self._read_file(self.path))
        elif self.path.is_dir():
            traces.extend(self._read_directory(self.path))
        else:
            logger.error(f"Path does not exist: {self.path}")
            raise FileNotFoundError(f"Path not found: {self.path}")
        
        return traces
    
    def _read_file(self, file_path: Path) -> List[Trace]:
        """Read traces from a single JSON file."""
        logger.info(f"Reading trace file: {file_path}")
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            traces = self._parse_json_data(data)
            
            # Set source name from filename
            source_name = file_path.stem
            for trace in traces:
                if not trace.source_name:
                    trace.source_name = source_name
            
            logger.info(f"Loaded {len(traces)} trace(s) from {file_path.name}")
            return traces
            
        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON in file {file_path}: {e}")
            return []
        except Exception as e:
            logger.error(f"Error reading file {file_path}: {e}")
            return []
    
    def _read_directory(self, dir_path: Path) -> List[Trace]:
        """Read traces from all JSON files in a directory."""
        logger.info(f"Reading trace files from directory: {dir_path}")
        
        all_traces = []
        json_files = list(dir_path.glob('*.json'))
        
        if not json_files:
            logger.warning(f"No JSON files found in directory: {dir_path}")
            return all_traces
        
        for json_file in json_files:
            all_traces.extend(self._read_file(json_file))
        
        return all_traces
    
    def _parse_json_data(self, data: dict) -> List[Trace]:
        """
        Parse JSON data into Trace objects.
        Handles different Jaeger JSON formats.
        """
        traces = []
        
        # Format 1: Jaeger API format with "data" field
        if isinstance(data, dict) and 'data' in data:
            trace_list = data['data']
            if isinstance(trace_list, list):
                for trace_data in trace_list:
                    traces.append(Trace.from_dict(trace_data))
        
        # Format 2: Direct array of traces
        elif isinstance(data, list):
            for trace_data in data:
                traces.append(Trace.from_dict(trace_data))
        
        # Format 3: Single trace object
        elif isinstance(data, dict) and 'traceID' in data:
            traces.append(Trace.from_dict(data))
        
        else:
            logger.warning("Unrecognized JSON format")
        
        return traces
