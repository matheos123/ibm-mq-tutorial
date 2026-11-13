package com.example.consumer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class AuditController {

    private final AuditService auditService;
    private final DlqService dlqService;

    public AuditController(AuditService auditService, DlqService dlqService) {
        this.auditService = auditService;
        this.dlqService = dlqService;
    }

    @GetMapping
    public List<AuditRecord> listMessages(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sender) {
        return auditService.getRecords(status, sender);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return auditService.getStats();
    }

    @GetMapping("/dlq")
    public List<AuditRecord> listDlqMessages() {
        return auditService.getRecords("DEAD_LETTER", null);
    }

    @PostMapping("/dlq/replay")
    public Map<String, Object> replayDlq() {
        int count = dlqService.replayAll();
        return Map.of(
            "replayed", count,
            "message", "Replayed " + count + " message(s) from DLQ back to main queue"
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> clear() {
        auditService.clear();
        return ResponseEntity.noContent().build();
    }
}

