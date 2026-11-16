package com.documentprocessor.messaging.consumer;

import com.documentprocessor.messaging.dto.DocumentMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Message consumer that listens for document processing requests on IBM MQ.
 */
@Component
public class DocumentMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentMessageConsumer.class);

    /**
     * Consumes incoming document processing requests from the request queue.
     * Extracts payload and JMS metadata (message ID, correlation ID) for verification and tracing.
     *
     * @param message the deserialized DocumentMessage payload
     * @param jmsMessage the underlying Jakarta JMS message containing headers and metadata
     * @throws JMSException if an error occurs reading JMS headers
     */
    @JmsListener(
            destination = "${app.mq.queues.request:DOCUMENT.REQUEST.QUEUE}",
            containerFactory = "jmsListenerContainerFactory"
    )
    public void receiveDocumentRequest(DocumentMessage message, Message jmsMessage) throws JMSException {
        String messageId = jmsMessage.getJMSMessageID();
        String correlationId = jmsMessage.getJMSCorrelationID();

        log.info("Received document processing request from IBM MQ: messageId={}, correlationId={}, documentId={}, file={}, type={}",
                messageId, correlationId, message.getDocumentId(), message.getDocumentName(), message.getDocumentType());
    }
}
