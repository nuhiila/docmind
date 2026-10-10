package com.nouhaila.docmind.document;

import com.nouhaila.docmind.user.User;
import com.nouhaila.docmind.user.UserRepository;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentAiController {

    public record SearchHit(String filename, Object page, Double score, String text) {}

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentQueue documentQueue;
    private final VectorStore vectorStore;

    public DocumentAiController(DocumentRepository documentRepository,
                                UserRepository userRepository,
                                DocumentQueue documentQueue,
                                VectorStore vectorStore) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.documentQueue = documentQueue;
        this.vectorStore = vectorStore;
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<?> process(@PathVariable Long id, Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName()).orElseThrow();
        Optional<Document> found = documentRepository.findByIdAndOwner(id, owner);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document not found");
        }
        Document document = found.get();
        if (document.getStatus() == DocumentStatus.READY || document.getStatus() == DocumentStatus.PROCESSING) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Document already processed or in progress");
        }
        document.setStatus(DocumentStatus.UPLOADED);
        documentRepository.save(document);
        documentQueue.enqueue(document.getId());
        return ResponseEntity.accepted().body(Map.of("documentId", id, "status", "QUEUED"));
    }

    @GetMapping("/search")
    public List<SearchHit> search(@RequestParam("q") String query, Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName()).orElseThrow();

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(4)
                .filterExpression(new FilterExpressionBuilder().eq("ownerId", owner.getId()).build())
                .build();

        List<org.springframework.ai.document.Document> results = vectorStore.similaritySearch(request);

        return results.stream()
                .map(d -> new SearchHit(
                        String.valueOf(d.getMetadata().get("filename")),
                        d.getMetadata().get("page"),
                        d.getScore(),
                        d.getText()))
                .toList();
    }
}