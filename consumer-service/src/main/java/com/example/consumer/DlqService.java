package com.example.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
public class DlqService {
    private static final Logger log = LoggerFactory.getLogger(DlqService.class);

    private final JmsTemplate jmsTemplate;
    private final String queueName;
    private final String dlqName;

    public DlqService(JmsTemplate jmsTemplate,
                      @Value("${app.queue}") String queueName,
                      @Value("${app.dlq}") String dlqName) {
        this.jmsTemplate = jmsTemplate;
        this.queueName = queueName;
        this.dlqName = dlqName;
    }

    public void sendToDlq(String rawPayload, String reason) {
        try {
            jmsTemplate.convertAndSend(dlqName, rawPayload, message -> {
                message.setStringProperty("DLQ_REASON", reason);
                message.setLongProperty("DLQ_TIMESTAMP", System.currentTimeMillis());
                return message;
            });
            log.warn("[DLQ] Poison message routed to {}: reason='{}'", dlqName, reason);
        } catch (Exception e) {
            log.error("[DLQ] Failed to forward message to {}: {}", dlqName, e.getMessage());
        }
    }

    public int replayAll() {
        int replayedCount = 0;
        jmsTemplate.setReceiveTimeout(1000);
        while (true) {
            Object item = jmsTemplate.receiveAndConvert(dlqName);
            if (item == null) {
                break;
            }
            jmsTemplate.convertAndSend(queueName, item);
            replayedCount++;
        }
        log.info("[DLQ] Replayed {} messages from {} back to {}", replayedCount, dlqName, queueName);
        return replayedCount;
    }
}

