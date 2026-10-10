package com.nouhaila.docmind.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nouhaila.docmind.user.User;
import com.nouhaila.docmind.user.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ask")
public class AskController {
    private static final Logger log = LoggerFactory.getLogger(AskController.class);
    private final RagService ragService;
    private final UserRepository userRepository;

    public AskController(RagService ragService, UserRepository userRepository) {
        this.ragService = ragService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> ask(@Valid @RequestBody AskRequest request, Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName()).orElseThrow();
        try {
            return ResponseEntity.ok(ragService.ask(request.question(), owner.getId()));
        } catch (RuntimeException e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("DEBUG: " + e.getClass().getSimpleName() + " / "
                            + root.getClass().getSimpleName() + ": " + root.getMessage());
        }
    }
}