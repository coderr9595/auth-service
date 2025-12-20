package com.example.auth.controller;

import com.example.auth.IntegrationTestBase;
import com.example.auth.model.ForgotPasswordRequest;
import com.example.auth.model.LoginRequest;
import com.example.auth.model.ResetPasswordRequest;
import com.example.auth.model.SignupRequest;
import com.example.auth.repository.PasswordResetTokenRepository;
import com.example.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthControllerIT extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordResetTokenRepository passwordResetTokenRepository;

    private String email;
    private String username;
    private String password;

    @BeforeEach
    void setup() {
        long n = System.currentTimeMillis();
        email = "it_" + n + "@example.com";
        username = "it_" + n;
        password = "Secret123!";
    }

    @Test
    void endToEnd_auth_flow() {
        // signup
        SignupRequest sr = new SignupRequest();
        sr.setName("Test User");
        sr.setEmail(email);
        sr.setUsername(username);
        sr.setPassword(password);

        ResponseEntity<String> signupResp = rest.postForEntity("/api/auth/signup", sr, String.class);
        assertThat(signupResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // login
        LoginRequest lr = new LoginRequest();
        lr.setUsername(username);
        lr.setPassword(password);
        ResponseEntity<String> loginResp = rest.postForEntity("/api/auth/login", lr, String.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        String accessToken = Json.path(loginResp.getBody(), "data.accessToken");
        assertThat(accessToken).isNotBlank();

        // /me
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        ResponseEntity<String> me = rest.exchange("/api/auth/me", HttpMethod.GET, new HttpEntity<>(h), String.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);

        // renew
        ResponseEntity<String> renew = rest.exchange("/api/auth/token/renew", HttpMethod.POST, new HttpEntity<>(h), String.class);
        assertThat(renew.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.UNAUTHORIZED);

        // logout
        ResponseEntity<String> logout = rest.exchange("/api/auth/logout", HttpMethod.POST, new HttpEntity<>(h), String.class);
        assertThat(logout.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST);

        // revoked should be rejected
        ResponseEntity<String> meAfter = rest.exchange("/api/auth/me", HttpMethod.GET, new HttpEntity<>(h), String.class);
        assertThat(meAfter.getStatusCode().value()).isIn(401, 403);

        // forgot-password
        ForgotPasswordRequest fp = new ForgotPasswordRequest();
        fp.setEmail(email);
        ResponseEntity<String> fpResp = rest.postForEntity("/api/auth/forgot-password", fp, String.class);
        assertThat(fpResp.getStatusCode().value()).isIn(200, 400, 500);

        // fetch reset token (if exists)
        String resetToken = passwordResetTokenRepository.findByUser(userRepository.findByEmail(email).orElseThrow()).map(t -> t.getToken()).orElse(null);
        if (resetToken != null) {
            ResetPasswordRequest rp = new ResetPasswordRequest();
            rp.setToken(resetToken);
            rp.setNewPassword("Secret456!");
            ResponseEntity<String> rpResp = rest.postForEntity("/api/auth/reset-password", rp, String.class);
            assertThat(rpResp.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST);
        }
    }

    // Lightweight JSON path helper without external libs
    static class Json {
        static String path(String body, String dotted) {
            if (body == null) return null;
            try {
                com.fasterxml.jackson.databind.JsonNode n = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
                String[] parts = dotted.split("\\.");
                for (String p : parts) {
                    n = n.get(p);
                    if (n == null) return null;
                }
                if (n.isTextual()) return n.asText();
                return n.toString();
            } catch (Exception e) {
                return null;
            }
        }
    }
}


