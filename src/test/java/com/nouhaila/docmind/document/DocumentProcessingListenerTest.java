package com.nouhaila.docmind.document;

import com.nouhaila.docmind.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingListenerTest {

    @Mock
    DocumentRepository documentRepository;

    @Mock
    DocumentProcessingService processingService;

    @InjectMocks
    DocumentProcessingListener listener;

    private Document documentWithStatus(DocumentStatus status) {
        Document document = new Document("a.pdf", "stored.pdf", 10, new User("user@example.com", "hash"));
        document.setStatus(status);
        return document;
    }

    @Test
    void processesAnUploadedDocument() {
        Document document = documentWithStatus(DocumentStatus.UPLOADED);
        when(documentRepository.findWithOwnerById(7L)).thenReturn(Optional.of(document));

        listener.onMessage("7");

        verify(processingService).process(document);
    }

    @Test
    void skipsADocumentThatIsAlreadyProcessed() {
        Document document = documentWithStatus(DocumentStatus.READY);
        when(documentRepository.findWithOwnerById(7L)).thenReturn(Optional.of(document));

        listener.onMessage("7");

        verify(processingService, never()).process(any());
    }

    @Test
    void skipsAMissingDocument() {
        when(documentRepository.findWithOwnerById(7L)).thenReturn(Optional.empty());

        listener.onMessage("7");

        verify(processingService, never()).process(any());
    }
}