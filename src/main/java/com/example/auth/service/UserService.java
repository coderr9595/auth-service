package com.example.auth.service;

import com.example.auth.model.*;
import com.example.auth.model.Enums.Role;
import com.example.auth.repository.PasswordResetTokenRepository;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service layer for user-related operations such as registration,
 * password reset initiation, and password update.
 */
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    /**
     * Registers a new user with the provided signup request.
     * Performs validation and handles username/email uniqueness.
     *
     * @param request User signup details
     * @return The newly created user
     * @throws IllegalArgumentException if input is invalid or user already exists
     */
    public User signup(SignupRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        String username;
        String email;
        
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            username = request.getUsername().trim();

            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                email = request.getEmail().trim();
                if (userRepository.existsByEmail(email)) {
                    throw new IllegalArgumentException("Email already exists");
                }
            } else {
                email = null;
            }
        } else {
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                throw new IllegalArgumentException("Either username or email must be provided");
            }
            
            email = request.getEmail().trim();
            username = email;
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        Role userRole = request.getRole() != null ? request.getRole() : Role.USER;

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(userRole);
        
        return userRepository.save(user);
    }

    /**
     * Initiates password reset process by generating a token
     * and sending a reset email to the user.
     *
     * @param request Contains the user's email
     * @throws IllegalArgumentException if user with the email doesn't exist
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User with this email does not exist"));

        passwordResetTokenRepository.deleteByUser(user);

        String resetToken = UUID.randomUUID().toString();

        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(5);

        PasswordResetToken passwordResetToken = new PasswordResetToken(resetToken, user, expiryDate);
        passwordResetTokenRepository.save(passwordResetToken);

        emailService.sendPasswordResetEmail(email, resetToken);
    }

    /**
     * Resets the user's password using a valid reset token.
     * Also marks the token as used and sends confirmation email.
     *
     * @param request Contains the token and new password
     * @throws IllegalArgumentException if the token is invalid, expired, or already used
     */
    public void resetPassword(ResetPasswordRequest request) {
        String token = request.getToken().trim();
        String newPassword = request.getNewPassword().trim();

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Reset token has expired");
        }

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Reset token has already been used");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetConfirmation(user.getEmail());

        passwordResetTokenRepository.deleteByUser(user);
    }

    /**
     * Deletes all expired password reset tokens from the database.
     */
    public void cleanupExpiredTokens() {
        passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}