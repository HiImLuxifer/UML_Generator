package com.uml.generator.util;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Utility class for generic name cleaning and normalization across different
 * microservice frameworks and deployment platforms.
 * 
 * Supports: gRPC, HTTP, Kafka, Kubernetes, Docker, VMs, and custom frameworks.
 */
public class NameUtils {

    // Common protocol prefixes to remove
    private static final Pattern PROTOCOL_PREFIX = Pattern
            .compile("^(grpc\\.|http\\.|https\\.|kafka\\.|amqp\\.|mqtt\\.)");

    // Common namespace patterns (Java, Go, etc.)
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("^([a-z][a-z0-9]*\\.)+");

    // Hash suffix patterns (common in K8s, Docker)
    private static final Pattern HASH_SUFFIX = Pattern.compile("-[a-f0-9]{8,}(-[a-z0-9]{5})?$");

    /**
     * Cleans operation names by removing framework-specific prefixes and
     * normalizing.
     * 
     * Examples:
     * - "grpc.hipstershop.ProductService/GetProduct" -> "ProductService/GetProduct"
     * - "/hipstershop.ProductService/ListProducts" -> "ProductService/ListProducts"
     * - "http.com.example.api.v1.UserService/getUser" -> "UserService/getUser"
     * 
     * @param operation Raw operation name from trace
     * @return Cleaned operation name
     */
    public static String cleanOperationName(String operation) {
        if (operation == null || operation.isEmpty()) {
            return operation;
        }

        String cleaned = operation.trim();

        // Remove leading slashes or backslashes
        cleaned = cleaned.replaceFirst("^[/\\\\]+", "");

        // Remove protocol prefixes (grpc., http., kafka., etc.)
        Matcher protocolMatcher = PROTOCOL_PREFIX.matcher(cleaned);
        if (protocolMatcher.find()) {
            cleaned = protocolMatcher.replaceFirst("");
        }

        // Detect and strip namespace/package prefixes
        cleaned = detectAndStripNamespace(cleaned);

        return cleaned;
    }

    /**
     * Cleans service names by normalizing across different naming conventions.
     * 
     * @param service Raw service name
     * @return Cleaned service name
     */
    public static String cleanServiceName(String service) {
        if (service == null || service.isEmpty()) {
            return service;
        }

        String cleaned = service.trim();

        // Remove protocol prefixes if present
        Matcher protocolMatcher = PROTOCOL_PREFIX.matcher(cleaned);
        if (protocolMatcher.find()) {
            cleaned = protocolMatcher.replaceFirst("");
        }

        return cleaned;
    }

    /**
     * Extracts base name by removing deployment hash suffixes.
     * Works with Kubernetes pod names, Docker container names, and custom patterns.
     * 
     * Examples:
     * - "recommendationservice-7d5c8f9b8-xk7pt" -> "recommendationservice"
     * - "frontend-abc123def456" -> "frontend"
     * - "my-service-v2-5f8d9c" -> "my-service-v2"
     * 
     * @param name Name with potential hash suffix
     * @return Base name without hash
     */
    public static String extractBaseName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // Remove hash suffixes
        Matcher hashMatcher = HASH_SUFFIX.matcher(name);
        if (hashMatcher.find()) {
            return hashMatcher.replaceFirst("");
        }

        return name;
    }

    /**
     * Detects and strips namespace/package prefixes automatically.
     * Handles common patterns from Java, Go, and other languages.
     * 
     * Examples:
     * - "com.example.ServiceName/method" -> "ServiceName/method"
     * - "org.microservices.v1.UserService/GetUser" -> "UserService/GetUser"
     * - "hipstershop.ProductService/List" -> "ProductService/List"
     * 
     * @param fullName Full name with potential namespace
     * @return Name with namespace removed
     */
    public static String detectAndStripNamespace(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return fullName;
        }

        // Look for namespace pattern before a service/class name
        // Pattern: lowercase.segments.CapitalizedName
        String pattern = "^([a-z][a-z0-9]*\\.)+([A-Z][a-zA-Z0-9]*)";
        Matcher matcher = Pattern.compile(pattern).matcher(fullName);

        if (matcher.find()) {
            // Extract everything after the namespace
            String namespace = matcher.group(0);
            String lastSegment = matcher.group(2); // The capitalized part

            // Replace the full namespace with just the last capitalized segment
            return fullName.replaceFirst("^([a-z][a-z0-9]*\\.)+", "");
        }

        // Alternative: simple dot-separated namespace removal if no capital letter
        // found
        // This handles cases like "service.Method" where we want "service.Method"
        // preserved
        // but "com.example.service.Method" should become "service.Method"
        if (fullName.contains(".")) {
            String[] parts = fullName.split("\\.");
            // If we have more than 2 segments and the first segments are lowercase, likely
            // a namespace
            if (parts.length > 2) {
                boolean hasNamespace = true;
                for (int i = 0; i < Math.min(parts.length - 1, 3); i++) {
                    if (!parts[i].matches("^[a-z][a-z0-9]*$")) {
                        hasNamespace = false;
                        break;
                    }
                }

                if (hasNamespace) {
                    // Find the first capitalized segment or keep last two segments
                    for (int i = 0; i < parts.length; i++) {
                        if (parts[i].matches("^[A-Z].*")) {
                            // Join from this point onwards
                            return String.join(".", java.util.Arrays.copyOfRange(parts, i, parts.length));
                        }
                    }
                    // No capitalized segment found, keep last two parts
                    if (parts.length >= 2) {
                        return parts[parts.length - 2] + "." + parts[parts.length - 1];
                    }
                }
            }
        }

        return fullName;
    }

    /**
     * Sanitizes a name to be used as a PlantUML identifier.
     * 
     * @param name Name to sanitize
     * @return Sanitized identifier safe for PlantUML
     */
    public static String sanitizeId(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
