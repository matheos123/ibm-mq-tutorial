package com.documentprocessor.document.enums;

/**
 * Lifecycle states of a document during asynchronous processing.
 */
public enum DocumentStatus {
    /**
     * Document submitted and persisted, awaiting processing by message consumer.
     */
    PENDING,

    /**
     * Document is currently being validated and processed.
     */
    PROCESSING,

    /**
     * Document processing and business validation completed successfully.
     */
    COMPLETED,

    /**
     * Document processing failed due to validation errors or system issues.
     */
    FAILED
}
