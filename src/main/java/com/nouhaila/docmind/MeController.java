package com.nouhaila.docmind;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public String me(Authentication authentication) {
        return "Hello " + authentication.getName();
    }
}