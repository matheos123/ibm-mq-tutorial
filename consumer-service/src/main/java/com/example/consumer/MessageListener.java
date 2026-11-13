package com.example.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {
    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    @JmsListener(destination = "${app.queue}")
    public void receive(String message) {
        log.info("[Consumer] Received: {}", message.trim());
    }
}
