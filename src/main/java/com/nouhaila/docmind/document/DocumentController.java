package com.nouhaila.docmind.document;

import com.nouhaila.docmind.user.User;
import com.nouhaila.docmind.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileStorageService storageService;

    public DocumentController(DocumentRepository documentRepository,
                              UserRepository userRepository,
                              FileStorageService storageService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    Authentication authentication) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body("Only PDF files are accepted");
        }

        User owner = userRepository.findByEmail(authentication.getName()).orElseThrow();
        String storedName = storageService.store(file);

        Document document = new Document(originalName, storedName, file.getSize(), owner);
        documentRepository.save(document);

        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(document));
    }

    @GetMapping
    public List<DocumentResponse> list(Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return documentRepository.findByOwnerOrderByUploadedAtDesc(owner).stream()
                .map(DocumentResponse::from)
                .toList();
    }
}