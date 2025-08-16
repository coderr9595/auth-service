package com.example.auth.service;

import com.example.auth.model.*;
import com.example.auth.repository.RevokedTokenRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Authentication service that implements stateless JWT access tokens.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RevokedTokenRepository revokedTokenRepository;

    /**
     * Validates credentials and returns the matching user.
     */
    public User authenticate(LoginRequest request) {
        if ((request.getUsername() == null || request.getUsername().isBlank()) &&
            (request.getEmail() == null || request.getEmail().isBlank())) {
            throw new IllegalArgumentException("Either username or email must be provided");
        }

        Optional<User> userOpt;
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            userOpt = userRepository.findByUsername(request.getUsername().trim());
        } else {
            userOpt = userRepository.findByEmail(request.getEmail().trim());
        }

        User user = userOpt.orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return user;
    }

    /**
     * Issues a short-lived JWT access token and returns user details.
     */
    public JwtLoginResponse login(LoginRequest request) {
        User user = authenticate(request);
        String access = jwtTokenProvider.generateToken(user.getUsername());
        return new JwtLoginResponse(access, jwtTokenProvider.getExpiryInstant(), UserResponse.fromUser(user));
    }

    /**
     * Renews the access token if the current token is valid and close to expiry.
     * Policy decision: allow renewal when token is still valid; clients call just before expiry.
     */
    public JwtLoginResponse renew(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        String token = bearerToken.substring(7);
        String jti = jwtTokenProvider.getJti(token);
        if (revokedTokenRepository.findByJti(jti).isPresent()) {
            throw new IllegalArgumentException("Token revoked");
        }
        String username = jwtTokenProvider.validateAndGetSubject(token);
        String newAccess = jwtTokenProvider.generateToken(username);
        return new JwtLoginResponse(newAccess, jwtTokenProvider.getExpiryInstant(), null);
    }

    /**
     * Logs out by revoking the current access token so it cannot be used anymore.
     */
    public void logout(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        String token = bearerToken.substring(7);
        String jti = jwtTokenProvider.getJti(token);
        Instant exp = jwtTokenProvider.getExpiration(token);
        revokedTokenRepository.findByJti(jti).orElseGet(() -> revokedTokenRepository.save(RevokedToken.of(jti, exp)));
    }
}
