package com.example.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Utility to generate and validate signed JWT access tokens.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final Key key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expirationMs:3600000}") long expirationMs
    ) {
        byte[] keyBytes;
        if (secret == null || secret.isBlank()) {
            // Generate a random key to allow startup in dev; tokens won't survive restart
            keyBytes = new byte[32];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("jwt.secret is missing; generated a temporary signing key. Set a persistent strong secret...");
        } else {
            try {
                // Try Base64 first
                keyBytes = Decoders.BASE64.decode(secret);
            } catch (RuntimeException e) {
                // Fallback to raw UTF-8 bytes if not valid Base64 (jjwt throws DecodingException)
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }

            if (keyBytes.length < 32) {
                try {
                    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                    keyBytes = sha256.digest(keyBytes);
                } catch (NoSuchAlgorithmException ex) {
                    throw new IllegalStateException("SHA-256 not available for key derivation", ex);
                }
            }
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }


    /**
     * Generates a signed JWT for the given subject (typically username) with a unique JTI.
     */
    public String generateToken(String subject) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Instant getExpiryInstant() {
        return Instant.now().plusMillis(expirationMs);
    }

    /**
     * Parses the token and returns the Claims (throws if invalid).
     */
    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validates the JWT and returns the subject (username) if valid.
     */
    public String validateAndGetSubject(String token) throws JwtException {
        return parseClaims(token).getSubject();
    }

    /** Returns the JTI from token */
    public String getJti(String token) { return parseClaims(token).getId(); }

    /** Returns the expiration instant from token */
    public Instant getExpiration(String token) { return parseClaims(token).getExpiration().toInstant(); }
}
