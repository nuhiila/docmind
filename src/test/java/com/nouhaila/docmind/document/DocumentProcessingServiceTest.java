package com.nouhaila.docmind.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import com.nouhaila.docmind.user.User;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    DocumentRepository documentRepository;

    @Mock
    VectorStore vectorStore;

    private FileStorageService storage;
    private DocumentProcessingService service;

    @BeforeEach
    void setUp() {
        storage = new FileStorageService(tempDir.toString());
        service = new DocumentProcessingService(documentRepository, storage, vectorStore);
    }

    private Document documentStoredAs(String storedName) {
        User owner = new User("user@example.com", "hash");
        ReflectionTestUtils.setField(owner, "id", 5L);
        Document document = new Document("original.pdf", storedName, 100, owner);
        ReflectionTestUtils.setField(document, "id", 1L);
        return document;
    }

    private void writePdf(Path path, String... pageTexts) throws IOException {
        try (PDDocument pdf = new PDDocument()) {
            for (String text : pageTexts) {
                PDPage page = new PDPage();
                pdf.addPage(page);
                if (!text.isEmpty()) {
                    try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                        content.beginText();
                        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        content.newLineAtOffset(50, 700);
                        content.showText(text);
                        content.endText();
                    }
                }
            }
            pdf.save(path.toFile());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void turnsEachPageIntoChunksWithMetadataAndMarksTheDocumentReady() throws IOException {
        writePdf(storage.resolve("test.pdf"), "First page about teaching", "Second page about teamwork");
        Document document = documentStoredAs("test.pdf");

        int chunkCount = service.process(document);

        ArgumentCaptor<List<org.springframework.ai.document.Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        List<org.springframework.ai.document.Document> chunks = captor.getValue();

        assertThat(chunkCount).isEqualTo(2);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getText()).contains("First page");
        assertThat(chunks.get(0).getMetadata())
                .containsEntry("page", 1)
                .containsEntry("documentId", 1L)
                .containsEntry("ownerId", 5L)
                .containsEntry("filename", "original.pdf");
        assertThat(chunks.get(1).getMetadata()).containsEntry("page", 2);
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.READY);
    }

    @Test
    void marksADocumentWithoutTextAsFailed() throws IOException {
        writePdf(storage.resolve("blank.pdf"), "");
        Document document = documentStoredAs("blank.pdf");

        assertThatThrownBy(() -> service.process(document))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No text found");

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void marksAFileThatIsNotAPdfAsFailed() throws IOException {
        Files.writeString(storage.resolve("bad.pdf"), "this is not a pdf");
        Document document = documentStoredAs("bad.pdf");

        assertThatThrownBy(() -> service.process(document))
                .isInstanceOf(IllegalStateException.class);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        verify(vectorStore, never()).add(anyList());
    }
}