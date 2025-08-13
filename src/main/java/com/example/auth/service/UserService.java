package com.example.auth.service;

import com.example.auth.model.*;
import com.example.auth.model.Enums.Role;
import lombok.RequiredArgsConstructor;
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
}