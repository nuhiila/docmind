package com.nouhaila.docmind.document;

import com.nouhaila.docmind.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class DocumentQueue {

    private final RabbitTemplate rabbitTemplate;

    public DocumentQueue(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enqueue(Long documentId) {
        rabbitTemplate.convertAndSend(RabbitConfig.PROCESSING_QUEUE, String.valueOf(documentId));
    }
}