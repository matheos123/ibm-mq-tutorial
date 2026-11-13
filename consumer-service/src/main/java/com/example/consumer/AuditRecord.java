package com.example.consumer;

import java.time.Instant;

public record AuditRecord(
    String id,
    String sender,
    String content,
    String priority,
    String status,
    String error,
    int attempts,
    String receivedAt
) {
    public static AuditRecord success(AppMessage message, int attempts) {
        return new AuditRecord(
            message.id(),
            message.sender(),
            message.content(),
            message.priority(),
            "PROCESSED",
            null,
            attempts,
            Instant.now().toString()
        );
    }

    public static AuditRecord deadLetter(AppMessage message, String error, int attempts) {
        return new AuditRecord(
            message != null ? message.id() : "unknown",
            message != null ? message.sender() : "unknown",
            message != null ? message.content() : "raw-payload",
            message != null ? message.priority() : "UNKNOWN",
            "DEAD_LETTER",
            error,
            attempts,
            Instant.now().toString()
        );
    }
}

