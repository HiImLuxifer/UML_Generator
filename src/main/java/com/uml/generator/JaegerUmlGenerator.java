package com.uml.generator;

import com.uml.generator.cli.CommandLineInterface;
import com.uml.generator.generator.*;
import com.uml.generator.input.*;
import com.uml.generator.model.Trace;
import com.uml.generator.renderer.XmiWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application class for Jaeger UML Generator.
 * Orchestrates reading traces and generating XMI diagrams.
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

        // Generate all requested diagram types for each trace individually
        for (int i = 0; i < traces.size(); i++) {
            Trace trace = traces.get(i);

            // Use sourceName if available, otherwise fall back to index
            String traceName = trace.getSourceName() != null && !trace.getSourceName().isEmpty()
                    ? trace.getSourceName()
                    : "trace-" + (i + 1);

            // Clean trace name by removing common prefixes
            traceName = cleanTraceName(traceName);

            // Create a list with single trace for individual diagram generation
            List<Trace> singleTraceList = new ArrayList<>();
            singleTraceList.add(trace);

            // Generate sequence diagram for this trace
            if (diagramType.equals("all") || diagramType.equals("sequence")) {
                logger.info("Generating sequence diagram for {}", traceName);
                SequenceDiagramGenerator sequenceGenerator = new SequenceDiagramGenerator();
                String xmiContent = sequenceGenerator.generateXmiForTrace(trace, i);

                if (xmiContent != null && !xmiContent.trim().isEmpty()) {
                    String filename = "sequence-" + traceName;
                    File xmiFile = new File(cli.getOutputDir(), filename + ".xmi");
                    saveXmiSource(xmiContent, xmiFile);
                    System.out.println("  Generated: " + xmiFile.getName());
                } else {
                    logger.warn("No XMI content generated for sequence diagram: {}", traceName);
                }
            }

            // Generate component diagram for this trace
            if (diagramType.equals("all") || diagramType.equals("component")) {
                logger.info("Generating component diagram for {}", traceName);
                ComponentDiagramGenerator componentGenerator = new ComponentDiagramGenerator();
                String xmiContent = componentGenerator.generateXmi(singleTraceList);

                if (xmiContent != null && !xmiContent.trim().isEmpty()) {
                    String filename = "component-" + traceName;
                    File xmiFile = new File(cli.getOutputDir(), filename + ".xmi");
                    saveXmiSource(xmiContent, xmiFile);
                    System.out.println("  Generated: " + xmiFile.getName());
                } else {
                    logger.warn("No XMI content generated for component diagram: {}", traceName);
                }
            }

            // Generate deployment diagram for this trace
            if (diagramType.equals("all") || diagramType.equals("deployment")) {
                logger.info("Generating deployment diagram for {}", traceName);
                DeploymentDiagramGenerator deploymentGenerator = new DeploymentDiagramGenerator();
                String xmiContent = deploymentGenerator.generateXmi(singleTraceList);

                if (xmiContent != null && !xmiContent.trim().isEmpty()) {
                    String filename = "deployment-" + traceName;
                    File xmiFile = new File(cli.getOutputDir(), filename + ".xmi");
                    saveXmiSource(xmiContent, xmiFile);
                    System.out.println("  Generated: " + xmiFile.getName());
                } else {
                    logger.warn("No XMI content generated for deployment diagram: {}", traceName);
                }
            }
        }
    }

    /**
     * Cleans trace name by removing common prefixes like "traccia_", "taccia_",
     * "trace_".
     * 
     * @param name the original trace name
     * @return cleaned trace name
     */
    private String cleanTraceName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // Remove common prefixes (case insensitive, with underscore or hyphen)
        String cleaned = name;

        // Try to match patterns like "traccia_xxx", "taccia_xxx", "trace_xxx"
        if (cleaned.toLowerCase().startsWith("traccia_") || cleaned.toLowerCase().startsWith("traccia-")) {
            cleaned = cleaned.substring(8); // Remove "traccia_" or "traccia-"
        } else if (cleaned.toLowerCase().startsWith("taccia_") || cleaned.toLowerCase().startsWith("taccia-")) {
            cleaned = cleaned.substring(7); // Remove "taccia_" or "taccia-"
        } else if (cleaned.toLowerCase().startsWith("trace_") || cleaned.toLowerCase().startsWith("trace-")) {
            cleaned = cleaned.substring(6); // Remove "trace_" or "trace-"
        }

        // If we removed everything, return original
        return cleaned.isEmpty() ? name : cleaned;
    }

    /**
     * Saves XMI content to a file.
     */
    private void saveXmiSource(String xmiContent, File file) throws Exception {
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write(xmiContent);
        }
        logger.info("Saved XMI file: {}", file.getName());
    }
}
