package com.example.auth.scheduled;

import com.example.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {
    
    private final UserService userService;
    
    /**
     * Scheduled task to clean up expired password reset tokens
     * Runs every 2 AM
     *
     * Cron format: second minute hour day month dayOfWeek
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of expired password reset tokens");
        
        try {
            long startTime = System.currentTimeMillis();
            
            userService.cleanupExpiredTokens();
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            log.info("Successfully cleaned up expired password reset tokens in {} ms", executionTime);
            
        } catch (Exception e) {
            log.error("Failed to cleanup expired password reset tokens", e);
        }
    }
    
    /**
     * Optional: Additional scheduled task for general maintenance
     * Runs daily at 2 AM: 0 0 2 * * *
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void dailyMaintenance() {
        log.info("Starting daily maintenance tasks");
        
        try {
            // Cleanup expired tokens
            userService.cleanupExpiredTokens();
            
            // Add other daily maintenance tasks here
            // Example: cleanup old logs, refresh caches, etc.
            
            log.info("Daily maintenance completed successfully");
            
        } catch (Exception e) {
            log.error("Daily maintenance failed", e);
        }
    }
}