package com.documentprocessor.messaging.config;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * IBM MQ and JMS configuration.
 * Configures JSON message conversion, persistent delivery QoS on JmsTemplate,
 * and transaction-aware JMS listener container factory.
 */
@Configuration
@EnableJms
public class IbmMqConfig {

    /**
     * Configure Jackson-based MessageConverter for serializing Java objects
     * to and from JSON TextMessages over IBM MQ.
     */
    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    /**
     * Configure JmsTemplate with persistent delivery mode and explicit QoS enabled.
     */
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setDeliveryMode(DeliveryMode.PERSISTENT);
        template.setExplicitQosEnabled(true);
        template.setSessionTransacted(true);
        return template;
    }

    /**
     * Configure DefaultJmsListenerContainerFactory with transactional session
     * and JSON message conversion support.
     */
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setSessionTransacted(true);
        return factory;
    }
}
