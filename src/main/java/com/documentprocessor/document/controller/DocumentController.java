package com.documentprocessor.document.controller;

import com.documentprocessor.document.dto.DocumentRequest;
import com.documentprocessor.document.dto.DocumentResponse;
import com.documentprocessor.document.service.DocumentService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing endpoints for document submission and retrieval.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Submit a new document for processing.
     *
     * @param request the validated document submission payload
     * @return HTTP 201 Created with the persisted document details
     */
    @PostMapping
    public ResponseEntity<DocumentResponse> submitDocument(@Valid @RequestBody DocumentRequest request) {
        DocumentResponse response = documentService.createDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve all submitted documents.
     *
     * @return HTTP 200 OK with the list of documents
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        List<DocumentResponse> documents = documentService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }

    /**
     * Retrieve a specific document by its unique identifier.
     *
     * @param id the document UUID
     * @return HTTP 200 OK with the document details
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable UUID id) {
        DocumentResponse response = documentService.getDocumentById(id);
        return ResponseEntity.ok(response);
    }
}
