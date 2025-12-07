package com.uml.generator.renderer;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Renders PlantUML source code to PNG images.
 */
public class PlantUmlRenderer {

    private static final Logger logger = LoggerFactory.getLogger(PlantUmlRenderer.class);

    /**
     * Renders PlantUML source to a PNG file.
     * 
     * @param plantUmlSource the PlantUML source code
     * @param outputFile     the output PNG file
     * @throws IOException if rendering fails
     */
    public void renderToPng(String plantUmlSource, File outputFile) throws IOException {
        if (plantUmlSource == null || plantUmlSource.trim().isEmpty()) {
            throw new IllegalArgumentException("PlantUML source cannot be empty");
        }

        logger.info("Rendering PlantUML to PNG: {}", outputFile.getAbsolutePath());

        try {
            SourceStringReader reader = new SourceStringReader(plantUmlSource);

            // Ensure parent directory exists
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Render to file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                reader.outputImage(fos, new FileFormatOption(FileFormat.PNG));
            }

            logger.info("Successfully rendered PNG: {}", outputFile.getName());

        } catch (Exception e) {
            logger.error("Failed to render PlantUML to PNG", e);
            throw new IOException("Failed to render PlantUML: " + e.getMessage(), e);
        }
    }

    /**
     * Renders PlantUML source to a PNG byte array.
     * 
     * @param plantUmlSource the PlantUML source code
     * @return PNG image as byte array
     * @throws IOException if rendering fails
     */
    public byte[] renderToPngBytes(String plantUmlSource) throws IOException {
        if (plantUmlSource == null || plantUmlSource.trim().isEmpty()) {
            throw new IllegalArgumentException("PlantUML source cannot be empty");
        }

        try {
            SourceStringReader reader = new SourceStringReader(plantUmlSource);

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                reader.outputImage(baos, new FileFormatOption(FileFormat.PNG));
                return baos.toByteArray();
            }

        } catch (Exception e) {
            logger.error("Failed to render PlantUML to PNG bytes", e);
            throw new IOException("Failed to render PlantUML: " + e.getMessage(), e);
        }
    }

    /**
     * Validates PlantUML source syntax.
     * 
     * @param plantUmlSource the PlantUML source code
     * @return true if valid
     */
    public boolean validate(String plantUmlSource) {
        if (plantUmlSource == null || plantUmlSource.trim().isEmpty()) {
            return false;
        }

        try {
            SourceStringReader reader = new SourceStringReader(plantUmlSource);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                reader.outputImage(baos, new FileFormatOption(FileFormat.PNG));
            }
            return true;
        } catch (Exception e) {
            logger.warn("Invalid PlantUML source: {}", e.getMessage());
            return false;
        }
    }
}
