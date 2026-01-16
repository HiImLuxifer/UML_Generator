package com.uml.generator.generator;

import com.uml.generator.model.Span;
import com.uml.generator.model.Trace;
import com.uml.generator.renderer.XmiWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates UML Sequence Diagrams in XMI 2.5.1 format from Jaeger traces.
 * Each trace produces one sequence diagram showing the flow of calls.
 */
public class SequenceDiagramGenerator implements DiagramGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SequenceDiagramGenerator.class);
    private static final String UML_NAMESPACE = "http://www.eclipse.org/uml2/5.0.0/UML";

    @Override
    public String getDiagramType() {
        return "sequence";
    }

    @Override
    public String generateXmi(List<Trace> traces) {
        if (traces == null || traces.isEmpty()) {
            logger.warn("No traces provided for sequence diagram generation");
            return "";
        }

        // For sequence diagrams, generate for the first trace
        // (multiple traces would require multiple XMI files)
        return generateXmiForTrace(traces.get(0), 0);
    }

    /**
     * Generates XMI for a single trace.
     * This method is public to allow generating separate diagrams for each trace.
     */
    public String generateXmiForTrace(Trace trace, int index) {
        if (trace == null) {
            logger.warn("Null trace provided for sequence diagram generation");
            return "";
        }

        try {
            XmiWriter writer = new XmiWriter();

            // Determine model name
            String modelName = "SequenceDiagram";
            if (trace.getSourceName() != null) {
                modelName = trace.getSourceName() + "_Sequence";
            }

            // Create XMI document
            Document doc = writer.createXmiDocument(modelName);
            Element model = writer.getModelElement(doc);

            // Create Interaction element
            Element interaction = doc.createElementNS(UML_NAMESPACE, "packagedElement");
            interaction.setAttribute("xmi:type", "uml:Interaction");
            String interactionId = XmiWriter.generateUUID();
            interaction.setAttribute("xmi:id", interactionId);
            interaction.setAttribute("name", modelName + "_Interaction");
            model.appendChild(interaction);

            // Get all services and create lifelines
            List<String> services = trace.getAllServiceNames();
            Map<String, String> serviceToLifelineId = new HashMap<>();

            for (String service : services) {
                String lifelineId = createLifeline(doc, interaction, service);
                serviceToLifelineId.put(service, lifelineId);
            }

            // Process spans and create messages
            List<Span> sortedSpans = trace.getSpansSortedByTime();
            Map<String, String> spanToService = new HashMap<>();

            for (Span span : sortedSpans) {
                spanToService.put(span.getSpanID(), trace.getServiceName(span));
            }

            int messageCounter = 0;
            int totalMessages = 0;

            for (Span span : sortedSpans) {
                String currentService = trace.getServiceName(span);
                String parentSpanId = span.getParentSpanId();

                if (parentSpanId != null) {
                    String parentService = spanToService.get(parentSpanId);

                    if (parentService != null && !parentService.equals(currentService)) {
                        // Cross-service call
                        String fromLifelineId = serviceToLifelineId.get(parentService);
                        String toLifelineId = serviceToLifelineId.get(currentService);

                        if (fromLifelineId != null && toLifelineId != null) {
                            String cleanOperation = com.uml.generator.util.NameUtils
                                    .cleanOperationName(span.getOperationName());

                            createMessage(doc, interaction, "msg" + messageCounter,
                                    cleanOperation, fromLifelineId, toLifelineId,
                                    "synchCall", span.getDuration());

                            messageCounter++;
                            totalMessages++;
                        }
                    }
                }
            }

            logger.info("Generated sequence diagram XMI for trace {} with {} participants and {} messages",
                    trace.getTraceID(), services.size(), totalMessages);

            return writer.documentToString(doc);

        } catch (Exception e) {
            logger.error("Failed to generate sequence diagram XMI", e);
            return "";
        }
    }

    /**
     * Creates a UML Lifeline element.
     */
    private String createLifeline(Document doc, Element interaction, String serviceName) {
        Element lifeline = doc.createElementNS(UML_NAMESPACE, "lifeline");
        String lifelineId = XmiWriter.generateUUID();
        lifeline.setAttribute("xmi:id", lifelineId);
        lifeline.setAttribute("name", serviceName);

        // Create represented element (the classifier this lifeline represents)
        lifeline.setAttribute("represents", serviceName);

        interaction.appendChild(lifeline);
        return lifelineId;
    }

    /**
     * Creates a UML Message with send and receive events.
     */
    private void createMessage(Document doc, Element interaction, String messageName,
            String operationName, String fromLifelineId, String toLifelineId,
            String messageSort, long duration) {

        // Create send event
        Element sendEvent = doc.createElementNS(UML_NAMESPACE, "fragment");
        sendEvent.setAttribute("xmi:type", "uml:MessageOccurrenceSpecification");
        String sendEventId = XmiWriter.generateUUID();
        sendEvent.setAttribute("xmi:id", sendEventId);
        sendEvent.setAttribute("name", messageName + "_send");
        sendEvent.setAttribute("covered", fromLifelineId);
        interaction.appendChild(sendEvent);

        // Create receive event
        Element receiveEvent = doc.createElementNS(UML_NAMESPACE, "fragment");
        receiveEvent.setAttribute("xmi:type", "uml:MessageOccurrenceSpecification");
        String receiveEventId = XmiWriter.generateUUID();
        receiveEvent.setAttribute("xmi:id", receiveEventId);
        receiveEvent.setAttribute("name", messageName + "_receive");
        receiveEvent.setAttribute("covered", toLifelineId);
        interaction.appendChild(receiveEvent);

        // Create message
        Element message = doc.createElementNS(UML_NAMESPACE, "message");
        message.setAttribute("xmi:id", XmiWriter.generateUUID());
        message.setAttribute("name", operationName);
        message.setAttribute("messageSort", messageSort);
        message.setAttribute("sendEvent", sendEventId);
        message.setAttribute("receiveEvent", receiveEventId);

        // Add timing information as a comment if available
        if (duration > 0) {
            Element comment = doc.createElementNS(UML_NAMESPACE, "ownedComment");
            comment.setAttribute("xmi:id", XmiWriter.generateUUID());
            Element body = doc.createElementNS(UML_NAMESPACE, "body");
            body.setTextContent("Duration: " + duration + "μs");
            comment.appendChild(body);
            message.appendChild(comment);
        }

        interaction.appendChild(message);
    }
}
