package com.nouhaila.docmind.document;

import com.nouhaila.docmind.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentProcessingListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingListener.class);

    private final DocumentRepository documentRepository;
    private final DocumentProcessingService processingService;

    public DocumentProcessingListener(DocumentRepository documentRepository,
                                      DocumentProcessingService processingService) {
        this.documentRepository = documentRepository;
        this.processingService = processingService;
    }

    @RabbitListener(queues = RabbitConfig.PROCESSING_QUEUE)
    public void onMessage(String documentId) {
        Long id = Long.valueOf(documentId);

        Document document = documentRepository.findWithOwnerById(id).orElse(null);
        if (document == null) {
            log.warn("Document {} not found, skipping", id);
            return;
        }
        if (document.getStatus() != DocumentStatus.UPLOADED) {
            log.info("Document {} is {}, skipping", id, document.getStatus());
            return;
        }

        log.info("Processing document {}", id);
        processingService.process(document);
        log.info("Document {} is READY", id);
    }
}