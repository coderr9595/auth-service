package com.example.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response body for successful JWT login operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtLoginResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private Instant expiresAt;
    private UserResponse user;

    public JwtLoginResponse(String accessToken, Instant expiresAt, UserResponse user) {
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
        this.user = user;
    }
}
