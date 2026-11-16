package com.documentprocessor.messaging.consumer;

import com.documentprocessor.document.service.DocumentProcessingService;
import com.documentprocessor.messaging.dto.DocumentMessage;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Message consumer that listens for document processing requests on IBM MQ.
 * Runs within a transacted JMS session with automated backout threshold handling:
 * - Successful processing commits the transaction.
 * - Transient failures trigger transaction rollback and redelivery.
 * - Poison pills reaching the backout threshold are intercepted, isolated in DOCUMENT.BACKOUT.QUEUE,
 *   and safely removed from the request queue.
 */
@Component
public class DocumentMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentMessageConsumer.class);

    private final DocumentProcessingService documentProcessingService;
    private final JmsTemplate jmsTemplate;
    private final String backoutQueue;
    private final int backoutThreshold;

    public DocumentMessageConsumer(
            DocumentProcessingService documentProcessingService,
            JmsTemplate jmsTemplate,
            @Value("${app.mq.queues.backout:DOCUMENT.BACKOUT.QUEUE}") String backoutQueue,
            @Value("${app.mq.backout-threshold:3}") int backoutThreshold) {
        this.documentProcessingService = documentProcessingService;
        this.jmsTemplate = jmsTemplate;
        this.backoutQueue = backoutQueue;
        this.backoutThreshold = backoutThreshold;
    }

    /**
     * Consumes incoming document processing requests from the request queue.
     * Evaluates redelivery attempts against backoutThreshold (BOTHRESH):
     * routes failing messages to the backout queue once the threshold is reached.
     *
     * @param message the deserialized DocumentMessage payload
     * @param jmsMessage the underlying Jakarta JMS message containing headers and metadata
     * @throws JMSException if an error occurs reading JMS headers
     * @throws RuntimeException if processing encounters a technical failure below the backout threshold
     */
    @JmsListener(
            destination = "${app.mq.queues.request:DOCUMENT.REQUEST.QUEUE}",
            containerFactory = "jmsListenerContainerFactory"
    )
    public void receiveDocumentRequest(DocumentMessage message, Message jmsMessage) throws JMSException {
        String messageId = jmsMessage.getJMSMessageID();
        String correlationId = jmsMessage.getJMSCorrelationID();
        int deliveryCount = jmsMessage.propertyExists("JMSXDeliveryCount")
                ? jmsMessage.getIntProperty("JMSXDeliveryCount")
                : 1;

        log.info("Processing request received: messageId={}, correlationId={}, documentId={}, file={}, type={}, deliveryCount={}",
                messageId, correlationId, message.getDocumentId(), message.getDocumentName(), message.getDocumentType(), deliveryCount);

        try {
            documentProcessingService.processDocument(message, messageId, deliveryCount);
            log.info("Successfully processed and committed message: messageId={}, documentId={}",
                    messageId, message.getDocumentId());
        } catch (RuntimeException ex) {
            if (deliveryCount >= backoutThreshold) {
                log.warn("Message reached backout threshold (deliveryCount={}/{}). Routing messageId={}, documentId={} to backout queue='{}'",
                        deliveryCount, backoutThreshold, messageId, message.getDocumentId(), backoutQueue);

                routeToBackoutQueue(message, messageId, correlationId, deliveryCount, ex.getMessage());
                // Successfully isolated into backout queue; acknowledge to remove from request queue
                return;
            }

            log.error("Technical failure processing messageId={}, documentId={}, deliveryCount={}/{}. Triggering rollback for redelivery: {}",
                    messageId, message.getDocumentId(), deliveryCount, backoutThreshold, ex.getMessage());
            throw ex; // Triggers JMS rollback and increment of JMSXDeliveryCount by IBM MQ
        }
    }

    private void routeToBackoutQueue(DocumentMessage message, String messageId, String correlationId,
                                     int deliveryCount, String failureReason) {
        jmsTemplate.convertAndSend(backoutQueue, message, jmsMsg -> {
            jmsMsg.setJMSCorrelationID(correlationId);
            jmsMsg.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
            jmsMsg.setStringProperty("originalMessageId", messageId != null ? messageId : "");
            jmsMsg.setIntProperty("deliveryCount", deliveryCount);
            jmsMsg.setStringProperty("failureReason", failureReason != null ? failureReason : "Exceeded backout threshold");
            return jmsMsg;
        });

        log.info("Poison message routed to backout queue successfully: documentId={}, backoutQueue={}",
                message.getDocumentId(), backoutQueue);
    }
}
