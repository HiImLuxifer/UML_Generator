package com.uml.generator.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uml.generator.model.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads Jaeger traces from JSON files.
 * Supports both single files and directories containing multiple JSON files.
 */
public class JsonFileReader implements TraceReader {

    private static final Logger logger = LoggerFactory.getLogger(JsonFileReader.class);

    private final ObjectMapper objectMapper;
    private final List<File> files;

    /**
     * Constructor for reading a single file.
     * 
     * @param filePath path to the JSON file
     */
    public JsonFileReader(String filePath) {
        this.objectMapper = new ObjectMapper();
        this.files = new ArrayList<>();

        File file = new File(filePath);
        if (file.isDirectory()) {
            this.files.addAll(loadJsonFilesFromDirectory(file));
        } else {
            this.files.add(file);
        }
    }

    /**
     * Constructor for reading multiple files.
     * 
     * @param files list of JSON files
     */
    public JsonFileReader(List<File> files) {
        this.objectMapper = new ObjectMapper();
        this.files = new ArrayList<>(files);
    }

    @Override
    public List<Trace> readTraces() throws Exception {
        List<Trace> traces = new ArrayList<>();

        for (File file : files) {
            logger.info("Reading traces from file: {}", file.getAbsolutePath());

            try {
                List<Trace> fileTraces = readTracesFromFile(file);
                traces.addAll(fileTraces);
                logger.info("Successfully read {} trace(s) from {}", fileTraces.size(), file.getName());
            } catch (Exception e) {
                logger.error("Failed to read traces from file: {}", file.getAbsolutePath(), e);
                throw new Exception("Failed to read traces from file: " + file.getName(), e);
            }
        }

        return traces;
    }

    /**
     * Reads traces from a single JSON file.
     * Handles both single trace and array of traces formats.
     * 
     * @param file the JSON file
     * @return list of traces
     * @throws IOException if reading fails
     */
    private List<Trace> readTracesFromFile(File file) throws IOException {
        List<Trace> traces = new ArrayList<>();

        JsonNode rootNode = objectMapper.readTree(file);

        // Check if the JSON is wrapped in a "data" field (Jaeger API format)
        if (rootNode.has("data")) {
            JsonNode dataNode = rootNode.get("data");

            if (dataNode.isArray()) {
                // Array of traces
                for (JsonNode traceNode : dataNode) {
                    Trace trace = objectMapper.treeToValue(traceNode, Trace.class);
                    traces.add(trace);
                }
            } else {
                // Single trace
                Trace trace = objectMapper.treeToValue(dataNode, Trace.class);
                traces.add(trace);
            }
        } else if (rootNode.isArray()) {
            // Direct array of traces
            for (JsonNode traceNode : rootNode) {
                Trace trace = objectMapper.treeToValue(traceNode, Trace.class);
                traces.add(trace);
            }
        } else {
            // Single trace object
            Trace trace = objectMapper.treeToValue(rootNode, Trace.class);
            traces.add(trace);
        }

        return traces;
    }

    /**
     * Loads all JSON files from a directory (non-recursive).
     * 
     * @param directory the directory
     * @return list of JSON files
     */
    private List<File> loadJsonFilesFromDirectory(File directory) {
        try (Stream<Path> paths = Files.walk(directory.toPath(), 1)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".json"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to load JSON files from directory: {}", directory.getAbsolutePath(), e);
            return new ArrayList<>();
        }
    }

    public int getFileCount() {
        return files.size();
    }
}
