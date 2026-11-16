package com.documentprocessor.messaging.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JMS Message Data Transfer Object published to IBM MQ queues
 * representing a document processing request.
 */
public class DocumentMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID documentId;
    private String documentName;
    private String documentType;
    private String correlationId;
    private LocalDateTime submissionTimestamp;

    public DocumentMessage() {
    }

    public DocumentMessage(UUID documentId, String documentName, String documentType,
                           String correlationId, LocalDateTime submissionTimestamp) {
        this.documentId = documentId;
        this.documentName = documentName;
        this.documentType = documentType;
        this.correlationId = correlationId;
        this.submissionTimestamp = submissionTimestamp;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public LocalDateTime getSubmissionTimestamp() {
        return submissionTimestamp;
    }

    public void setSubmissionTimestamp(LocalDateTime submissionTimestamp) {
        this.submissionTimestamp = submissionTimestamp;
    }

    @Override
    public String toString() {
        return "DocumentMessage{" +
                "documentId=" + documentId +
                ", documentName='" + documentName + '\'' +
                ", documentType='" + documentType + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", submissionTimestamp=" + submissionTimestamp +
                '}';
    }
}
