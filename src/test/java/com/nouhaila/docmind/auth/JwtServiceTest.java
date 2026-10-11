package com.nouhaila.docmind.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-characters-long";
    private static final String OTHER_SECRET = "another-secret-that-is-also-at-least-32-characters";

    @Test
    void tokenContainsTheEmailItWasIssuedFor() {
        JwtService service = new JwtService(SECRET, 60_000);

        String token = service.generateToken("user@example.com");

        assertThat(service.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtService(SECRET, 60_000).generateToken("user@example.com");
        JwtService other = new JwtService(OTHER_SECRET, 60_000);

        assertThatThrownBy(() -> other.extractEmail(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = new JwtService(SECRET, -1_000);
        String token = service.generateToken("user@example.com");

        assertThatThrownBy(() -> service.extractEmail(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService service = new JwtService(SECRET, 60_000);
        String token = service.generateToken("user@example.com");
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> service.extractEmail(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void garbageIsRejected() {
        JwtService service = new JwtService(SECRET, 60_000);

        assertThatThrownBy(() -> service.extractEmail("not-a-jwt")).isInstanceOf(JwtException.class);
    }
}