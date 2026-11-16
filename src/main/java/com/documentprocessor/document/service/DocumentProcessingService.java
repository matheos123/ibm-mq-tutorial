package com.documentprocessor.document.service;

import com.documentprocessor.document.entity.Document;
import com.documentprocessor.document.enums.DocumentStatus;
import com.documentprocessor.document.repository.DocumentRepository;
import com.documentprocessor.log.entity.ProcessingLog;
import com.documentprocessor.log.repository.ProcessingLogRepository;
import com.documentprocessor.messaging.dto.DocumentMessage;
import jakarta.jms.DeliveryMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * Validates document types and simulates document processing.
 * idempotent duplicate protection, and audit history logging in PostgreSQL.
 * Publishes completion events to DOCUMENT.SUCCESS.QUEUE
 */
@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    private static final Set<String> SUPPORTED_DOCUMENT_TYPES = Set.of(
            "INVOICE",
            "FINANCIAL_REPORT",
            "LEGAL_CONTRACT",
            "PDF",
            "DOCX",
            "TXT"
    );

    private final DocumentRepository documentRepository;
    private final ProcessingLogRepository processingLogRepository;
    private final JmsTemplate jmsTemplate;
    private final String successQueue;

    public DocumentProcessingService(
            DocumentRepository documentRepository,
            ProcessingLogRepository processingLogRepository,
            JmsTemplate jmsTemplate,
            @Value("${app.mq.queues.success:DOCUMENT.SUCCESS.QUEUE}") String successQueue) {
        this.documentRepository = documentRepository;
        this.processingLogRepository = processingLogRepository;
        this.jmsTemplate = jmsTemplate;
        this.successQueue = successQueue;
    }

    /**
     * Overload for callers without explicit JMS messageId and retryCount.
     */
    @Transactional
    public void processDocument(DocumentMessage message) {
        processDocument(message, null, 0);
    }

    /**
     * Executes validation and processing for a consumed document message.
     * Enforces idempotency to protect against duplicate message processing.
     *
     * @param message the document processing request message
     * @param messageId the IBM MQ / JMS message ID
     * @param retryCount current delivery attempt count
     * @throws RuntimeException if a technical failure or poison pill is simulated
     */
    @Transactional
    public void processDocument(DocumentMessage message, String messageId, int retryCount) {
        log.info("Starting processing for documentId={}, correlationId={}, retryCount={}",
                message.getDocumentId(), message.getCorrelationId(), retryCount);

        Document document = documentRepository.findById(message.getDocumentId())
                .orElseThrow(() -> new NoSuchElementException("Document not found: " + message.getDocumentId()));

        // Duplicate Protection (Idempotency Check)
        if (DocumentStatus.COMPLETED.name().equals(document.getStatus())) {
            log.warn("Duplicate processing request ignored: documentId={} is already in COMPLETED status.",
                    document.getId());
            saveAuditLog(document.getId(), messageId, message.getCorrelationId(),
                    DocumentStatus.COMPLETED.name(), retryCount, "Duplicate processing ignored: document already completed");
            return;
        }

        // Update status to PROCESSING and log audit entry
        document.setStatus(DocumentStatus.PROCESSING.name());
        documentRepository.save(document);
        saveAuditLog(document.getId(), messageId, message.getCorrelationId(),
                DocumentStatus.PROCESSING.name(), retryCount, null);

        // Technical failure simulation (poison pill triggering rollback)
        String fileName = (message.getDocumentName() != null) ? message.getDocumentName().toLowerCase() : "";
        String docType = (message.getDocumentType() != null) ? message.getDocumentType().toUpperCase() : "";

        if (fileName.contains("poison") || fileName.contains("corrupt") || "POISON".equals(docType)) {
            String errorMsg = "Simulated technical failure (poison pill) for document: " + message.getDocumentId();
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // Business validation check (unsupported document types)
        if (!SUPPORTED_DOCUMENT_TYPES.contains(docType)) {
            String validationError = String.format("Unsupported document type '%s'. Supported types: %s",
                    docType, SUPPORTED_DOCUMENT_TYPES);
            log.warn("Business validation rejected documentId={}: {}", message.getDocumentId(), validationError);

            document.setStatus(DocumentStatus.FAILED.name());
            documentRepository.save(document);
            saveAuditLog(document.getId(), messageId, message.getCorrelationId(),
                    DocumentStatus.FAILED.name(), retryCount, validationError);
            return;
        }

        // Simulate document processing delay
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Document processing interrupted", e);
        }

        // Processing completed successfully
        document.setStatus(DocumentStatus.COMPLETED.name());
        documentRepository.save(document);
        saveAuditLog(document.getId(), messageId, message.getCorrelationId(),
                DocumentStatus.COMPLETED.name(), retryCount, null);
        log.info("Document successfully processed and marked COMPLETED: documentId={}", document.getId());

        // Publish success event
        publishSuccessEvent(message);
    }

    /**
     * Retrieve the chronological processing audit history for a document.
     *
     * @param documentId the ID of the document
     * @return chronological list of processing audit log records
     */
    @Transactional(readOnly = true)
    public List<ProcessingLog> getProcessingHistory(UUID documentId) {
        return processingLogRepository.findByDocumentIdOrderByCreatedAtAsc(documentId);
    }

    private void saveAuditLog(UUID documentId, String messageId, String correlationId,
                              String status, int retryCount, String errorMessage) {
        ProcessingLog auditLog = new ProcessingLog(
                documentId,
                messageId,
                correlationId,
                status,
                retryCount,
                errorMessage
        );
        processingLogRepository.save(auditLog);
    }

    private void publishSuccessEvent(DocumentMessage message) {
        log.info("Publishing success event to queue={}: documentId={}, correlationId={}",
                successQueue, message.getDocumentId(), message.getCorrelationId());

        jmsTemplate.convertAndSend(successQueue, message, jmsMessage -> {
            jmsMessage.setJMSCorrelationID(message.getCorrelationId());
            jmsMessage.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
            jmsMessage.setStringProperty("documentId", message.getDocumentId().toString());
            jmsMessage.setStringProperty("status", DocumentStatus.COMPLETED.name());
            return jmsMessage;
        });
    }
}
