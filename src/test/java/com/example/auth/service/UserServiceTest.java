package com.example.auth.service;

import com.example.auth.IntegrationTestBase;
import com.example.auth.model.ForgotPasswordRequest;
import com.example.auth.model.ResetPasswordRequest;
import com.example.auth.model.SignupRequest;
import com.example.auth.model.User;
import com.example.auth.repository.PasswordResetTokenRepository;
import com.example.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserServiceTest extends IntegrationTestBase {

    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired PasswordEncoder encoder;

    @Test
    void signup_and_reset_flow() {
        String suffix = String.valueOf(System.currentTimeMillis());
        String email = "ut_" + suffix + "@example.com";
        String username = "ut_" + suffix;

        SignupRequest sr = new SignupRequest();
        sr.setName("Unit User");
        sr.setEmail(email);
        sr.setUsername(username);
        sr.setPassword("Secret123!");

        User u = userService.signup(sr);
        assertThat(u.getId()).isNotNull();
        assertThat(u.getEmail()).isEqualTo(email);
        assertThat(encoder.matches("Secret123!", u.getPassword())).isTrue();

        // forgot password creates token
        ForgotPasswordRequest fp = new ForgotPasswordRequest();
        fp.setEmail(email);
        userService.forgotPassword(fp);
        var token = passwordResetTokenRepository.findByUser(u).orElseThrow();
        assertThat(token.isExpired()).isFalse();
        assertThat(token.isUsed()).isFalse();

        // reset password with token
        ResetPasswordRequest rp = new ResetPasswordRequest();
        rp.setToken(token.getToken());
        rp.setNewPassword("Secret456!");
        userService.resetPassword(rp);

        User u2 = userRepository.findById(u.getId()).orElseThrow();
        assertThat(encoder.matches("Secret456!", u2.getPassword())).isTrue();

        // token is marked used and then cleaned
        assertThat(passwordResetTokenRepository.findByUser(u2)).isEmpty();

        // duplicate username/email should error
        assertThatThrownBy(() -> userService.signup(sr)).isInstanceOf(IllegalArgumentException.class);
    }
}
