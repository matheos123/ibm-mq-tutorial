package com.documentprocessor.document.repository;

import com.documentprocessor.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for Document entity operations.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Find all documents matching a specific status (e.g., PENDING, PROCESSING, COMPLETED, FAILED).
     *
     * @param status the status to search for
     * @return list of matching documents
     */
    List<Document> findByStatus(String status);

    /**
     * Find all documents matching a specific document type.
     *
     * @param documentType the document type to search for
     * @return list of matching documents
     */
    List<Document> findByDocumentType(String documentType);
}
