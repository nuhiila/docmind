package com.nouhaila.docmind.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String PROCESSING_QUEUE = "document.processing";
    public static final String DEAD_LETTER_QUEUE = "document.processing.dlq";

    @Bean
    public Queue processingQueue() {
        return QueueBuilder.durable(PROCESSING_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }
}