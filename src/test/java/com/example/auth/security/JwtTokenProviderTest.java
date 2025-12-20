package com.example.auth.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtTokenProviderTest {

    @Test
    void generate_and_parse_token() {
        JwtTokenProvider provider = new JwtTokenProvider("test-secret-which-is-long-enough-1234567890123456", 1000L);
        String token = provider.generateToken("alice");
        assertThat(token).isNotBlank();
        String subject = provider.validateAndGetSubject(token);
        assertThat(subject).isEqualTo("alice");
        String jti = provider.getJti(token);
        assertThat(jti).isNotBlank();
        Instant exp = provider.getExpiration(token);
        assertThat(exp).isAfter(Instant.now());
    }
}


