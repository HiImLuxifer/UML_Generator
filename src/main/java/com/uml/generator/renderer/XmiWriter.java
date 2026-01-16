package com.uml.generator.renderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.StringWriter;
import java.util.UUID;

/**
 * Writes XMI 2.5.1 files conforming to OMG UML 2.5.1 specification.
 * Optimized for Eclipse Papyrus compatibility.
 */
public class XmiWriter {

    private static final Logger logger = LoggerFactory.getLogger(XmiWriter.class);

    // XMI 2.5.1 namespaces
    private static final String XMI_NAMESPACE = "http://www.omg.org/spec/XMI/20131001";
    private static final String UML_NAMESPACE = "http://www.eclipse.org/uml2/5.0.0/UML";
    private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * Creates a new XMI document with proper XMI 2.5.1 structure.
     * 
     * @param modelName the name of the UML model
     * @return DOM Document ready for UML elements
     * @throws Exception if document creation fails
     */
    public Document createXmiDocument(String modelName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // Create root XMI element
        Element xmiRoot = doc.createElementNS(XMI_NAMESPACE, "xmi:XMI");
        xmiRoot.setAttribute("xmlns:xmi", XMI_NAMESPACE);
        xmiRoot.setAttribute("xmlns:uml", UML_NAMESPACE);
        xmiRoot.setAttribute("xmlns:xsi", XSI_NAMESPACE);
        xmiRoot.setAttribute("xmi:version", "2.5.1");
        doc.appendChild(xmiRoot);

        // Create UML Model element
        Element model = doc.createElementNS(UML_NAMESPACE, "uml:Model");
        model.setAttribute("xmi:id", generateUUID());
        model.setAttribute("name", modelName);
        xmiRoot.appendChild(model);

        logger.debug("Created XMI document for model: {}", modelName);
        return doc;
    }

    /**
     * Writes an XMI document to a file.
     * 
     * @param doc        the XMI document
     * @param outputFile the output file
     * @throws Exception if writing fails
     */
    public void writeToFile(Document doc, File outputFile) throws Exception {
        // Ensure parent directory exists
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Configure transformer for pretty-printing
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(outputFile);

        transformer.transform(source, result);

        logger.info("Successfully wrote XMI file: {}", outputFile.getAbsolutePath());
    }

    /**
     * Converts an XMI document to a string.
     * 
     * @param doc the XMI document
     * @return XML string representation
     * @throws Exception if conversion fails
     */
    public String documentToString(Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        DOMSource source = new DOMSource(doc);

        transformer.transform(source, result);

        return writer.toString();
    }

    /**
     * Generates a unique XMI ID conforming to NCName requirements.
     * Uses UUID with underscore prefix to ensure valid XML ID.
     * 
     * @return unique XMI-compliant ID
     */
    public static String generateUUID() {
        return "_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Gets the UML Model element from an XMI document.
     * 
     * @param doc the XMI document
     * @return the UML Model element
     */
    public Element getModelElement(Document doc) {
        return (Element) doc.getElementsByTagNameNS(UML_NAMESPACE, "Model").item(0);
    }

    /**
     * Creates a UML packaged element.
     * 
     * @param doc  the XMI document
     * @param type the UML type (e.g., "Component", "Interface", "Dependency")
     * @param name the element name
     * @return the created element
     */
    public Element createPackagedElement(Document doc, String type, String name) {
        Element element = doc.createElementNS(UML_NAMESPACE, "packagedElement");
        element.setAttribute("xmi:type", "uml:" + type);
        element.setAttribute("xmi:id", generateUUID());
        if (name != null && !name.isEmpty()) {
            element.setAttribute("name", name);
        }
        return element;
    }

    /**
     * Validates that an XMI document has proper structure.
     * 
     * @param doc the XMI document
     * @return true if valid
     */
    public boolean validate(Document doc) {
        try {
            // Check for XMI root
            if (!doc.getDocumentElement().getLocalName().equals("XMI")) {
                logger.warn("Document root is not xmi:XMI");
                return false;
            }

            // Check for UML Model
            if (doc.getElementsByTagNameNS(UML_NAMESPACE, "Model").getLength() == 0) {
                logger.warn("No uml:Model element found");
                return false;
            }

            logger.debug("XMI document validation passed");
            return true;

        } catch (Exception e) {
            logger.error("XMI validation failed", e);
            return false;
        }
    }
}
