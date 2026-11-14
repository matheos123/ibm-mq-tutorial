package com.documentprocessor.log.repository;

import com.documentprocessor.log.entity.ProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for ProcessingLog audit records.
 */
@Repository
public interface ProcessingLogRepository extends JpaRepository<ProcessingLog, UUID> {

    /**
     * Retrieve all processing logs for a specific document, ordered chronologically.
     * Used by the GET /api/documents/{id}/history endpoint.
     *
     * @param documentId the ID of the document
     * @return chronological list of processing log entries
     */
    List<ProcessingLog> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);

    /**
     * Retrieve all processing logs for a specific document.
     *
     * @param documentId the ID of the document
     * @return list of processing log entries
     */
    List<ProcessingLog> findByDocumentId(UUID documentId);

    /**
     * Retrieve logs matching a specific IBM MQ / JMS correlation ID.
     *
     * @param correlationId the JMS correlation ID
     * @return list of matching processing log entries
     */
    List<ProcessingLog> findByCorrelationId(String correlationId);

    /**
     * Retrieve logs matching a specific IBM MQ / JMS message ID.
     *
     * @param messageId the JMS message ID
     * @return list of matching processing log entries
     */
    List<ProcessingLog> findByMessageId(String messageId);
}
