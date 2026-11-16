package com.documentprocessor.messaging.producer;

import com.documentprocessor.messaging.dto.DocumentMessage;
import jakarta.jms.DeliveryMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Message producer responsible for publishing document processing requests
 * to IBM MQ with persistent delivery and correlation tracking.
 */
@Component
public class DocumentMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(DocumentMessageProducer.class);

    private final JmsTemplate jmsTemplate;
    private final String requestQueue;

    public DocumentMessageProducer(
            JmsTemplate jmsTemplate,
            @Value("${app.mq.queues.request:DOCUMENT.REQUEST.QUEUE}") String requestQueue) {
        this.jmsTemplate = jmsTemplate;
        this.requestQueue = requestQueue;
    }

    /**
     * Publishes a document processing request message to the IBM MQ request queue.
     *
     * @param message the document payload and metadata to publish
     */
    public void sendProcessingRequest(DocumentMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("DocumentMessage cannot be null");
        }

        if (message.getCorrelationId() == null || message.getCorrelationId().isBlank()) {
            message.setCorrelationId(UUID.randomUUID().toString());
        }

        log.info("Publishing document processing request: documentId={}, correlationId={}, queue={}",
                message.getDocumentId(), message.getCorrelationId(), requestQueue);

        jmsTemplate.convertAndSend(requestQueue, message, jmsMessage -> {
            jmsMessage.setJMSCorrelationID(message.getCorrelationId());
            jmsMessage.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
            jmsMessage.setStringProperty("documentId", message.getDocumentId().toString());
            jmsMessage.setStringProperty("documentType", message.getDocumentType());
            return jmsMessage;
        });

        log.info("Successfully published message for documentId={} with correlationId={}",
                message.getDocumentId(), message.getCorrelationId());
    }

    public String getRequestQueue() {
        return requestQueue;
    }
}
