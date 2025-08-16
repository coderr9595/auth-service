package com.example.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service for sending password reset-related emails.
 * <p>
 * Uses {@link JavaMailSender} for sending emails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Value("${spring.mail.username:noreply@authservice.com}")
    private String fromEmail;

    /**
     * Sends a password reset email to the user with a unique reset link.
     *
     * @param toEmail Recipient's email address
     * @param resetToken Token to be included in the reset link
     * @throws RuntimeException if email sending fails
     */
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset Request");
            
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
            
            String emailBody = String.format(
                "Dear User,\n\n" +
                "You have requested to reset your password. Please click the link below to reset your password:\n\n" +
                "%s\n\n" +
                "This link will expire in 15 minutes.\n\n" +
                "If you did not request this password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Auth Service Team",
                resetUrl
            );
            
            message.setText(emailBody);
            
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email");
        }
    }

    /**
     * Sends a confirmation email after a successful password reset.
     *
     * @param toEmail Recipient's email address
     */

    public void sendPasswordResetConfirmation(String toEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset Successful");
            
            String emailBody = 
                "Dear User,\n\n" +
                "Your password has been successfully reset.\n\n" +
                "If you did not perform this action, please contact our support team immediately.\n\n" +
                "Best regards,\n" +
                "Auth Service Team";
            
            message.setText(emailBody);
            
            mailSender.send(message);
            log.info("Password reset confirmation email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send password reset confirmation email to: {}", toEmail, e);
        }
    }
} 