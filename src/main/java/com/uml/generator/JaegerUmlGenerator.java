package com.uml.generator;

import com.uml.generator.cli.CommandLineInterface;
import com.uml.generator.generator.*;
import com.uml.generator.input.*;
import com.uml.generator.model.Trace;
import com.uml.generator.renderer.PlantUmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application class for Jaeger UML Generator.
 * Orchestrates reading traces, generating diagrams, and rendering to PNG.
 */
public class JaegerUmlGenerator {

    private static final Logger logger = LoggerFactory.getLogger(JaegerUmlGenerator.class);

    public static void main(String[] args) {
        // Parse command line arguments
        CommandLineInterface cli = CommandLineInterface.parseArguments(args);

        if (cli == null) {
            System.exit(1);
        }

        // Validate CLI
        try {
            int validationResult = cli.call();
            if (validationResult != 0) {
                System.exit(validationResult);
            }
        } catch (Exception e) {
            logger.error("CLI validation failed", e);
            System.exit(1);
        }

        // Execute generation
        JaegerUmlGenerator generator = new JaegerUmlGenerator();

        try {
            generator.generate(cli);
            System.out.println("\n✓ Successfully generated UML diagrams in: " + cli.getOutputDir().getAbsolutePath());
            System.exit(0);
        } catch (Exception e) {
            logger.error("Failed to generate UML diagrams", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Main generation logic.
     */
    public void generate(CommandLineInterface cli) throws Exception {
        logger.info("Starting Jaeger UML Generator");

        // Step 1: Read traces
        List<Trace> traces = readTraces(cli);

        if (traces.isEmpty()) {
            throw new Exception("No traces found");
        }

        logger.info("Loaded {} trace(s)", traces.size());

        // Step 2: Generate diagrams
        generateDiagrams(cli, traces);

        logger.info("Diagram generation complete");
    }

    /**
     * Reads traces from the configured input source.
     */
    private List<Trace> readTraces(CommandLineInterface cli) throws Exception {
        TraceReader reader;

        if (cli.getInputFile() != null) {
            logger.info("Reading traces from file: {}", cli.getInputFile().getAbsolutePath());
            reader = new JsonFileReader(cli.getInputFile().getAbsolutePath());

        } else if (cli.getInputDir() != null) {
            logger.info("Reading traces from directory: {}", cli.getInputDir().getAbsolutePath());
            reader = new JsonFileReader(cli.getInputDir().getAbsolutePath());

        } else if (cli.getJaegerUrl() != null) {
            logger.info("Reading traces from Jaeger API: {}", cli.getJaegerUrl());
            reader = new JaegerApiClient(
                    cli.getJaegerUrl(),
                    cli.getServiceName(),
                    null,
                    cli.getLimit());

        } else {
            throw new Exception("No input source specified");
        }

        return reader.readTraces();
    }

    /**
     * Generates diagrams based on CLI configuration.
     */
    private void generateDiagrams(CommandLineInterface cli, List<Trace> traces) throws Exception {
        String diagramType = cli.getDiagramType().toLowerCase();

        List<DiagramGenerator> generators = new ArrayList<>();

        // Determine which generators to use
        if (diagramType.equals("all") || diagramType.equals("sequence")) {
            generators.add(new SequenceDiagramGenerator());
        }

        if (diagramType.equals("all") || diagramType.equals("component")) {
            generators.add(new ComponentDiagramGenerator());
        }

        if (diagramType.equals("all") || diagramType.equals("deployment")) {
            generators.add(new DeploymentDiagramGenerator());
        }

        if (generators.isEmpty()) {
            throw new Exception("Invalid diagram type: " + cli.getDiagramType());
        }

        // Generate and render each diagram type
        PlantUmlRenderer renderer = new PlantUmlRenderer();

        for (DiagramGenerator generator : generators) {
            logger.info("Generating {} diagram", generator.getDiagramType());

            String plantUmlSource = generator.generatePlantUML(traces);

            if (plantUmlSource == null || plantUmlSource.trim().isEmpty()) {
                logger.warn("No PlantUML source generated for {} diagram", generator.getDiagramType());
                continue;
            }

            // Save PlantUML source
            File pumlFile = new File(cli.getOutputDir(), generator.getDiagramType() + "-diagram.puml");
            savePlantUmlSource(plantUmlSource, pumlFile);

            // Render to PNG
            File pngFile = new File(cli.getOutputDir(), generator.getDiagramType() + "-diagram.png");
            renderer.renderToPng(plantUmlSource, pngFile);

            System.out.println("  Generated: " + pngFile.getName());
        }
    }

    /**
     * Saves PlantUML source code to a file.
     */
    private void savePlantUmlSource(String source, File file) throws Exception {
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write(source);
        }
        logger.info("Saved PlantUML source: {}", file.getName());
    }
}
