package com.example.consumer;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {
    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);
    private static final int MAX_ATTEMPTS = 3;

    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final DlqService dlqService;

    public MessageListener(ObjectMapper objectMapper,
                           AuditService auditService,
                           DlqService dlqService) {
        this.objectMapper = objectMapper;
        this.auditService = auditService;
        this.dlqService = dlqService;
    }

    @JmsListener(destination = "${app.queue}")
    public void receive(String messageText) {
        AppMessage message = null;
        try {
            message = objectMapper.readValue(messageText, AppMessage.class);
        } catch (Exception e) {
            log.error("[Consumer] Malformed payload received: {}", messageText);
            dlqService.sendToDlq(messageText, "Malformed JSON payload: " + e.getMessage());
            auditService.recordDeadLetter(null, "Malformed JSON: " + e.getMessage(), 1);
            return;
        }

        // Business validation / Simulated poison pill
        if (message.content() != null && message.content().toUpperCase().contains("FAIL")) {
            log.warn("[Consumer] Message [id={}] failed business validation. Retrying up to {} times...",
                    message.id(), MAX_ATTEMPTS);

            int attempts = 0;
            for (int i = 1; i <= MAX_ATTEMPTS; i++) {
                attempts = i;
                log.warn("[Consumer] Retry attempt {}/{} for message [id={}]...", i, MAX_ATTEMPTS, message.id());
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {}
            }

            String error = "Business rule failure: message flagged with 'FAIL'";
            log.error("[Consumer] Retries exhausted for message [id={}]. Routing to DLQ.", message.id());
            dlqService.sendToDlq(messageText, error);
            auditService.recordDeadLetter(message, error, attempts);
            return;
        }

        // Successfully processed
        log.info("[Consumer] Processed: [id={}] [from={}] [priority={}] content='{}'",
                message.id(), message.sender(), message.priority(), message.content());
        auditService.recordSuccess(message, 1);
    }
}
