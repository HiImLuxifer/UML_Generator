"""
Main application for Jaeger UML Generator.
Orchestrates reading traces and generating XMI diagrams.
"""

import logging
import sys
from pathlib import Path
from typing import List

from .models import Trace
from .input import JsonFileReader, JaegerApiClient, TraceReader
from .generators import (
    SequenceDiagramGenerator,
    ComponentDiagramGenerator,
    DeploymentDiagramGenerator
)
from .cli import CommandLine
from .utils import clean_trace_name


# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class JaegerUmlGenerator:
    """Main application class for Jaeger UML Generator."""
    
    def __init__(self, cli: CommandLine):
        """
        Initialize the generator.
        
        Args:
            cli: Command-line interface object
        """
        self.cli = cli
        
        # Set logging level
        if cli.is_verbose():
            logging.getLogger().setLevel(logging.DEBUG)
    
    def generate(self):
        """Main generation logic."""
        logger.info("Starting Jaeger UML Generator")
        
        # Step 1: Read traces
        traces = self._read_traces()
        
        if not traces:
            raise Exception("No traces found")
        
        logger.info(f"Loaded {len(traces)} trace(s)")
        
        # Step 2: Generate diagrams
        self._generate_diagrams(traces)
        
        logger.info("Diagram generation complete")
    
    def _read_traces(self) -> List[Trace]:
        """Read traces from the configured input source."""
        reader: TraceReader
        
        if self.cli.get_input_file():
            logger.info(f"Reading traces from file: {self.cli.get_input_file()}")
            reader = JsonFileReader(self.cli.get_input_file())
            
        elif self.cli.get_input_dir():
            logger.info(f"Reading traces from directory: {self.cli.get_input_dir()}")
            reader = JsonFileReader(self.cli.get_input_dir())
            
        elif self.cli.get_jaeger_url():
            logger.info(f"Reading traces from Jaeger API: {self.cli.get_jaeger_url()}")
            reader = JaegerApiClient(
                self.cli.get_jaeger_url(),
                self.cli.get_service_name(),
                self.cli.get_lookback(),
                self.cli.get_limit()
            )
            
        else:
            raise Exception("No input source specified")
        
        return reader.read_traces()
    
    def _generate_diagrams(self, traces: List[Trace]):
        """Generate diagrams based on CLI configuration."""
        diagram_type = self.cli.get_diagram_type().lower()
        output_dir = self.cli.get_output_dir()
        xmi_format = self.cli.get_xmi_format()
        
        # Generate all requested diagram types for each trace individually
        for i, trace in enumerate(traces):
            # Use sourceName if available, otherwise fall back to index
            trace_name = trace.source_name if trace.source_name else f"trace-{i + 1}"
            
            # Clean trace name
            trace_name = clean_trace_name(trace_name)
            
            # Create a list with single trace for individual diagram generation
            single_trace_list = [trace]
            
            # Generate sequence diagram for this trace
            if diagram_type in ['all', 'sequence']:
                logger.info(f"Generating sequence diagram for {trace_name}")
                generator = SequenceDiagramGenerator(xmi_format)
                xmi_content = generator.generate_xmi_for_trace(trace, i)
                
                if xmi_content and xmi_content.strip():
                    filename = f"sequence-{trace_name}.xmi"
                    xmi_file = output_dir / filename
                    self._save_xmi(xmi_content, xmi_file)
                    print(f"  Generated: {filename}")
                else:
                    logger.warning(f"No XMI content generated for sequence diagram: {trace_name}")
            
            # Generate component diagram for this trace
            if diagram_type in ['all', 'component']:
                logger.info(f"Generating component diagram for {trace_name}")
                generator = ComponentDiagramGenerator(xmi_format)
                xmi_content = generator.generate_xmi(single_trace_list)
                
                if xmi_content and xmi_content.strip():
                    filename = f"component-{trace_name}.xmi"
                    xmi_file = output_dir / filename
                    self._save_xmi(xmi_content, xmi_file)
                    print(f"  Generated: {filename}")
                else:
                    logger.warning(f"No XMI content generated for component diagram: {trace_name}")
            
            # Generate deployment diagram for this trace
            if diagram_type in ['all', 'deployment']:
                logger.info(f"Generating deployment diagram for {trace_name}")
                generator = DeploymentDiagramGenerator(xmi_format)
                xmi_content = generator.generate_xmi(single_trace_list)
                
                if xmi_content and xmi_content.strip():
                    filename = f"deployment-{trace_name}.xmi"
                    xmi_file = output_dir / filename
                    self._save_xmi(xmi_content, xmi_file)
                    print(f"  Generated: {filename}")
                else:
                    logger.warning(f"No XMI content generated for deployment diagram: {trace_name}")
    
    def _save_xmi(self, xmi_content: str, file_path: Path):
        """
        Save XMI content to a file.
        
        Args:
            xmi_content: XMI content string
            file_path: Output file path
        """
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(xmi_content)
        logger.info(f"Saved XMI file: {file_path.name}")


def main(argv=None):
    """Main entry point."""
    # Parse command line arguments
    cli = CommandLine()
    if not cli.parse_args(argv):
        sys.exit(1)
    
    # Execute generation
    generator = JaegerUmlGenerator(cli)
    
    try:
        generator.generate()
        print(f"\n✓ Successfully generated UML diagrams in: {cli.get_output_dir()}")
        sys.exit(0)
    except Exception as e:
        logger.error(f"Failed to generate UML diagrams: {e}", exc_info=True)
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
