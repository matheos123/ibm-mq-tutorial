-- V1__create_document_tables.sql
-- Flyway migration: schema creation for documents and processing_logs

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_documents_status ON documents(status);

CREATE TABLE processing_logs (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    message_id VARCHAR(255),
    correlation_id VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_processing_logs_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_processing_logs_document_id ON processing_logs(document_id);
CREATE INDEX idx_processing_logs_correlation_id ON processing_logs(correlation_id);
