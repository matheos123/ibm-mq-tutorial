package com.documentprocessor.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for document submission requests.
 */
public class DocumentRequest {

    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name must not exceed 255 characters")
    private String fileName;

    @NotBlank(message = "Document type is required")
    @Size(max = 100, message = "Document type must not exceed 100 characters")
    private String documentType;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    public DocumentRequest() {
    }

    public DocumentRequest(String fileName, String documentType, String description) {
        this.fileName = fileName;
        this.documentType = documentType;
        this.description = description;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "DocumentRequest{" +
                "fileName='" + fileName + '\'' +
                ", documentType='" + documentType + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
