package com.example.producer;

import tools.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public MessageController(JmsTemplate jmsTemplate,
                             @Value("${app.queue}") String queue,
                             ObjectMapper objectMapper) {
        this.jmsTemplate = jmsTemplate;
        this.queue = queue;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/api/messages", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AppMessage sendJson(@RequestBody AppMessage message) {
        if (message.content() == null || message.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content must not be blank");
        }
        try {
            String json = objectMapper.writeValueAsString(message);
            jmsTemplate.convertAndSend(queue, json);
            log.info("[Producer] Sent: [id={}] [from={}] [priority={}] content='{}'",
                    message.id(), message.sender(), message.priority(), message.content());
            return message;
        } catch (JmsException exception) {
            log.error("[Producer] Failed to send message to IBM MQ: {}", exception.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not send message to IBM MQ", exception);
        } catch (Exception exception) {
            log.error("[Producer] Failed to serialize message: {}", exception.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not serialize message", exception);
        }
    }

    @PostMapping(value = "/api/messages", consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AppMessage sendPlainText(@RequestBody String text) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must not be blank");
        }
        AppMessage message = AppMessage.of(text.trim());
        return sendJson(message);
    }
}
