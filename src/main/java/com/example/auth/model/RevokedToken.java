package com.example.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Persisted record of revoked JWT access tokens identified by their JTI.
 * Used to invalidate tokens server-side prior to natural expiry.
 */
@Entity
@Table(name = "revoked_tokens", indexes = {
    @Index(name = "idx_revoked_token_jti", columnList = "jti", unique = true),
    @Index(name = "idx_revoked_token_expires_at", columnList = "expiresAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** JWT ID (jti claim) */
    @Column(nullable = false, unique = true)
    private String jti;

    /** When the original JWT would naturally expire */
    @Column(nullable = false)
    private Instant expiresAt;

    public static RevokedToken of(String jti, Instant expiresAt) {
        RevokedToken r = new RevokedToken();
        r.setJti(jti);
        r.setExpiresAt(expiresAt);
        return r;
    }
}
