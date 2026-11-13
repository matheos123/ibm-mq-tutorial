package com.example.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MessageController {
    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    private final JmsTemplate jmsTemplate;
    private final String queue;

    public MessageController(JmsTemplate jmsTemplate, @Value("${app.queue}") String queue) {
        this.jmsTemplate = jmsTemplate;
        this.queue = queue;
    }

    @PostMapping(value = "/api/messages", consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String send(@RequestBody String message) {
        if (message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must not be blank");
        }
        try {
            jmsTemplate.convertAndSend(queue, message);
            log.info("[Producer] Sent: {}", message.trim());
        } catch (JmsException exception) {
            log.error("[Producer] Failed to send message: {}", exception.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not send message to IBM MQ", exception);
        }
        return "Sent to " + queue + "\n";
    }
}
