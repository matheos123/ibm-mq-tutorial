package com.documentprocessor.document.service;

import com.documentprocessor.document.dto.DocumentRequest;
import com.documentprocessor.document.dto.DocumentResponse;
import com.documentprocessor.document.entity.Document;
import com.documentprocessor.document.enums.DocumentStatus;
import com.documentprocessor.document.repository.DocumentRepository;
import com.documentprocessor.messaging.dto.DocumentMessage;
import com.documentprocessor.messaging.producer.DocumentMessageProducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Service managing document creation, state transitions, message publishing, and retrieval.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentMessageProducer documentMessageProducer;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentMessageProducer documentMessageProducer) {
        this.documentRepository = documentRepository;
        this.documentMessageProducer = documentMessageProducer;
    }

    /**
     * Submit and persist a new document request with PENDING status, then publish
     * a processing request message to IBM MQ.
     * If publishing fails, the document is marked as FAILED to avoid a misleading PENDING state.
     *
     * @param request the document creation request
     * @return the created document details
     */
    @Transactional(noRollbackFor = IllegalStateException.class)
    public DocumentResponse createDocument(DocumentRequest request) {
        log.info("Creating new document: fileName={}, type={}", request.getFileName(), request.getDocumentType());

        Document document = new Document(
                request.getFileName(),
                request.getDocumentType(),
                request.getDescription(),
                DocumentStatus.PENDING.name()
        );

        Document saved = documentRepository.save(document);
        log.info("Document persisted successfully with ID: {}", saved.getId());

        String correlationId = UUID.randomUUID().toString();
        DocumentMessage message = new DocumentMessage(
                saved.getId(),
                saved.getFileName(),
                saved.getDocumentType(),
                correlationId,
                saved.getCreatedAt()
        );

        try {
            documentMessageProducer.sendProcessingRequest(message);
        } catch (Exception ex) {
            log.error("Failed to publish processing request to IBM MQ for documentId={}: {}",
                    saved.getId(), ex.getMessage(), ex);
            saved.setStatus(DocumentStatus.FAILED.name());
            documentRepository.save(saved);
            throw new IllegalStateException("Failed to queue document processing request: " + ex.getMessage(), ex);
        }

        return DocumentResponse.fromEntity(saved);
    }

    /**
     * Retrieve all persisted documents.
     *
     * @return list of document responses
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAllDocuments() {
        log.debug("Retrieving all documents");
        return documentRepository.findAll()
                .stream()
                .map(DocumentResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieve a document by its unique identifier.
     *
     * @param id the document UUID
     * @return the document details
     * @throws NoSuchElementException if document is not found
     */
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(UUID id) {
        log.debug("Retrieving document by ID: {}", id);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Document not found with ID: " + id));

        return DocumentResponse.fromEntity(document);
    }
}
