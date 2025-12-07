package com.uml.generator.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Command-line interface for the Jaeger UML Generator.
 */
@Command(name = "jaeger-uml-generator", description = "Generate UML diagrams from Jaeger distributed traces", mixinStandardHelpOptions = true, version = "1.0.0")
public class CommandLineInterface implements Callable<Integer> {

    @Option(names = { "-f", "--input-file" }, description = "Input JSON file containing Jaeger trace(s)")
    private File inputFile;

    @Option(names = { "-d", "--input-dir" }, description = "Input directory containing JSON trace files")
    private File inputDir;

    @Option(names = { "-j", "--jaeger-url" }, description = "Jaeger API URL (e.g., http://localhost:16686)")
    private String jaegerUrl;

    @Option(names = { "-s", "--service" }, description = "Filter traces by service name (for Jaeger API)")
    private String serviceName;

    @Option(names = { "-o",
            "--output-dir" }, description = "Output directory for generated diagrams (default: ./output)", defaultValue = "./output")
    private File outputDir;

    @Option(names = { "-t",
            "--diagram-type" }, description = "Diagram type: sequence, component, deployment, all (default: all)", defaultValue = "all")
    private String diagramType;

    @Option(names = { "-v", "--verbose" }, description = "Enable verbose logging")
    private boolean verbose;

    @Option(names = { "-l",
            "--limit" }, description = "Maximum number of traces to fetch from Jaeger API (default: 100)", defaultValue = "100")
    private int limit;

    @Override
    public Integer call() throws Exception {
        // Input validation
        if (inputFile == null && inputDir == null && jaegerUrl == null) {
            System.err.println("Error: Must specify one of --input-file, --input-dir, or --jaeger-url");
            return 1;
        }

        if ((inputFile != null ? 1 : 0) + (inputDir != null ? 1 : 0) + (jaegerUrl != null ? 1 : 0) > 1) {
            System.err.println("Error: Can only specify one of --input-file, --input-dir, or --jaeger-url");
            return 1;
        }

        if (inputFile != null && !inputFile.exists()) {
            System.err.println("Error: Input file does not exist: " + inputFile.getAbsolutePath());
            return 1;
        }

        if (inputDir != null && !inputDir.isDirectory()) {
            System.err.println(
                    "Error: Input directory does not exist or is not a directory: " + inputDir.getAbsolutePath());
            return 1;
        }

        // Set logging level
        if (verbose) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        }

        // Create output directory if it doesn't exist
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        return 0;
    }

    // Getters
    public File getInputFile() {
        return inputFile;
    }

    public File getInputDir() {
        return inputDir;
    }

    public String getJaegerUrl() {
        return jaegerUrl;
    }

    public String getServiceName() {
        return serviceName;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public String getDiagramType() {
        return diagramType;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public int getLimit() {
        return limit;
    }

    /**
     * Entry point for parsing command line arguments.
     */
    public static CommandLineInterface parseArguments(String[] args) {
        CommandLineInterface cli = new CommandLineInterface();
        CommandLine cmd = new CommandLine(cli);

        try {
            cmd.parseArgs(args);

            if (cmd.isUsageHelpRequested()) {
                cmd.usage(System.out);
                return null;
            }

            if (cmd.isVersionHelpRequested()) {
                cmd.printVersionHelp(System.out);
                return null;
            }

            return cli;

        } catch (CommandLine.ParameterException e) {
            System.err.println(e.getMessage());
            cmd.usage(System.err);
            return null;
        }
    }
}
