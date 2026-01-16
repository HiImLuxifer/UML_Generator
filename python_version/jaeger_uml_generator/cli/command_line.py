"""Command-line interface for Jaeger UML Generator."""

import argparse
import sys
from pathlib import Path
from typing import Optional


class CommandLine:
    """Handles command-line argument parsing and validation."""
    
    def __init__(self):
        self.parser = self._create_parser()
        self.args = None
    
    def _create_parser(self) -> argparse.ArgumentParser:
        """Create the argument parser."""
        parser = argparse.ArgumentParser(
            prog='jaeger-uml-generator',
            description='Generate UML diagrams from Jaeger distributed traces',
            formatter_class=argparse.RawDescriptionHelpFormatter,
            epilog='''
Examples:
  # Generate all diagrams from a JSON file
  python -m jaeger_uml_generator.main -f traces/trace.json -o output/
  
  # Generate only sequence diagrams from a directory
  python -m jaeger_uml_generator.main -d traces/ -t sequence -o diagrams/
  
  # Fetch traces from Jaeger API
  python -m jaeger_uml_generator.main \\
    -j http://localhost:16686 -s frontend -o output/
            '''
        )
        
        # Input sources (mutually exclusive)
        input_group = parser.add_mutually_exclusive_group(required=True)
        input_group.add_argument(
            '-f', '--input-file',
            type=str,
            help='Input JSON file containing Jaeger traces'
        )
        input_group.add_argument(
            '-d', '--input-dir',
            type=str,
            help='Input directory containing JSON trace files'
        )
        input_group.add_argument(
            '-j', '--jaeger-url',
            type=str,
            help='Jaeger API URL (e.g., http://localhost:16686)'
        )
        
        # Jaeger API specific options
        parser.add_argument(
            '-s', '--service',
            type=str,
            help='Service name to filter traces (used with --jaeger-url)'
        )
        parser.add_argument(
            '-l', '--limit',
            type=int,
            default=100,
            help='Maximum number of traces to fetch from Jaeger API (default: 100)'
        )
        parser.add_argument(
            '--lookback',
            type=str,
            default='24h',
            help='Lookback time for Jaeger API (default: 24h)'
        )
        
        # Output options
        parser.add_argument(
            '-o', '--output-dir',
            type=str,
            default='./output',
            help='Output directory for generated diagrams (default: ./output)'
        )
        parser.add_argument(
            '-t', '--diagram-type',
            type=str,
            choices=['all', 'sequence', 'component', 'deployment'],
            default='all',
            help='Type of diagram to generate (default: all)'
        )
        
        # Output format
        parser.add_argument(
            '--format',
            type=str,
            choices=['papyrus', 'magicdraw'],
            default='papyrus',
            help='XMI output format: papyrus (Eclipse) or magicdraw (default: papyrus)'
        )
        
        # Logging
        parser.add_argument(
            '-v', '--verbose',
            action='store_true',
            help='Enable verbose logging'
        )
        
        return parser
    
    def parse_args(self, argv: Optional[list] = None) -> bool:
        """
        Parse command-line arguments.
        
        Args:
            argv: List of argument strings (default: sys.argv[1:])
            
        Returns:
            True if parsing successful, False otherwise
        """
        try:
            self.args = self.parser.parse_args(argv)
            return self.validate()
        except SystemExit:
            return False
    
    def validate(self) -> bool:
        """
        Validate the parsed arguments.
        
        Returns:
            True if valid, False otherwise
        """
        # Validate input source exists
        if self.args.input_file:
            path = Path(self.args.input_file)
            if not path.exists():
                print(f"Error: Input file does not exist: {self.args.input_file}", file=sys.stderr)
                return False
            if not path.is_file():
                print(f"Error: Input path is not a file: {self.args.input_file}", file=sys.stderr)
                return False
        
        if self.args.input_dir:
            path = Path(self.args.input_dir)
            if not path.exists():
                print(f"Error: Input directory does not exist: {self.args.input_dir}", file=sys.stderr)
                return False
            if not path.is_dir():
                print(f"Error: Input path is not a directory: {self.args.input_dir}", file=sys.stderr)
                return False
        
        # Validate Jaeger API options
        if self.args.jaeger_url:
            if not self.args.jaeger_url.startswith(('http://', 'https://')):
                print("Error: Jaeger URL must start with http:// or https://", file=sys.stderr)
                return False
        
        # Create output directory if it doesn't exist
        output_path = Path(self.args.output_dir)
        try:
            output_path.mkdir(parents=True, exist_ok=True)
        except Exception as e:
            print(f"Error: Cannot create output directory: {e}", file=sys.stderr)
            return False
        
        return True
    
    def get_input_file(self) -> Optional[str]:
        """Get input file path."""
        return self.args.input_file if self.args else None
    
    def get_input_dir(self) -> Optional[str]:
        """Get input directory path."""
        return self.args.input_dir if self.args else None
    
    def get_jaeger_url(self) -> Optional[str]:
        """Get Jaeger API URL."""
        return self.args.jaeger_url if self.args else None
    
    def get_service_name(self) -> Optional[str]:
        """Get service name filter."""
        return self.args.service if self.args else None
    
    def get_limit(self) -> int:
        """Get trace limit."""
        return self.args.limit if self.args else 100
    
    def get_lookback(self) -> str:
        """Get lookback time."""
        return self.args.lookback if self.args else '24h'
    
    def get_output_dir(self) -> Path:
        """Get output directory path."""
        return Path(self.args.output_dir) if self.args else Path('./output')
    
    def get_diagram_type(self) -> str:
        """Get diagram type."""
        return self.args.diagram_type if self.args else 'all'
    
    def is_verbose(self) -> bool:
        """Check if verbose logging is enabled."""
        return self.args.verbose if self.args else False
    
    def get_xmi_format(self) -> str:
        """Get XMI output format (papyrus or magicdraw)."""
        return self.args.format if self.args else 'papyrus'
