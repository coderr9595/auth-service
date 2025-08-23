package com.example.auth.scheduled;

import com.example.auth.IntegrationTestBase;
import com.example.auth.model.PasswordResetToken;
import com.example.auth.model.User;
import com.example.auth.repository.PasswordResetTokenRepository;
import com.example.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenCleanupSchedulerTest extends IntegrationTestBase {

    @Autowired PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired UserRepository userRepository;

    @Test
    void cleanupExpiredTokens_removesOld() {
        User u = userRepository.save(new User("A","a@example.com","a","x"));
        PasswordResetToken t = new PasswordResetToken("tok", u, LocalDateTime.now().minusMinutes(10));
        passwordResetTokenRepository.save(t);
        passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        assertThat(passwordResetTokenRepository.findByUser(u)).isEmpty();
    }
}