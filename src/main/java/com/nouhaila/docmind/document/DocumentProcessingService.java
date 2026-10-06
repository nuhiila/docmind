package com.nouhaila.docmind.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final FileStorageService storageService;
    private final VectorStore vectorStore;

    public DocumentProcessingService(DocumentRepository documentRepository,
                                     FileStorageService storageService,
                                     VectorStore vectorStore) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.vectorStore = vectorStore;
    }

    public int process(Document document) {
        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);
        try {
            List<org.springframework.ai.document.Document> chunks = extractChunks(document);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("No text found in the PDF (scanned document?)");
            }
            vectorStore.add(chunks);
            document.setStatus(DocumentStatus.READY);
            documentRepository.save(document);
            return chunks.size();
        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
            throw new IllegalStateException("Processing failed: " + e.getMessage(), e);
        }
    }

    private List<org.springframework.ai.document.Document> extractChunks(Document document) throws IOException {
        List<org.springframework.ai.document.Document> chunks = new ArrayList<>();
        try (PDDocument pdf = Loader.loadPDF(storageService.resolve(document.getStoredName()).toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(pdf);
                for (String piece : TextChunker.chunk(text, 150, 30)) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("documentId", document.getId());
                    metadata.put("ownerId", document.getOwner().getId());
                    metadata.put("filename", document.getOriginalName());
                    metadata.put("page", page);
                    chunks.add(new org.springframework.ai.document.Document(piece, metadata));
                }
            }
        }
        return chunks;
    }
}