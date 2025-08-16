package com.example.auth.controller;

import com.example.auth.model.*;
import com.example.auth.service.AuthService;
import com.example.auth.service.UserService;
import com.example.auth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * Registers a new user.
     *
     * @param request Signup request data
     * @param bindingResult Validation result
     * @return Success response with user details or error message
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(@Valid @RequestBody SignupRequest request, BindingResult bindingResult) {
        try {
            // Handle validation errors
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.joining(", "));
                
                ApiResponse<UserResponse> response = ApiResponse.error(errorMessage);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            User user = userService.signup(request);
            UserResponse userResponse = UserResponse.fromUser(user);
            
            ApiResponse<UserResponse> response = ApiResponse.success("User registered successfully", userResponse);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            ApiResponse<UserResponse> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception e) {
            ApiResponse<UserResponse> response = ApiResponse.error("Internal server error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Initiates the password reset process by sending a reset email.
     *
     * @param request Forgot password request containing the user's email
     * @param bindingResult Validation result
     * @return Success response if email is sent, or error message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, BindingResult bindingResult) {
        try {
            // Handle validation errors
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.joining(", "));
                
                ApiResponse<String> response = ApiResponse.error(errorMessage);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            userService.forgotPassword(request);
            
            ApiResponse<String> response = ApiResponse.success("Password reset email sent successfully", null);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            ApiResponse<String> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception e) {
            ApiResponse<String> response = ApiResponse.error("Failed to send password reset email");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Resets the user's password using a reset token.
     *
     * @param request Reset password request with token and new password
     * @param bindingResult Validation result
     * @return Success response if password is reset, or error message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request, BindingResult bindingResult) {
        try {
            // Handle validation errors
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.joining(", "));
                
                ApiResponse<String> response = ApiResponse.error(errorMessage);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            userService.resetPassword(request);
            
            ApiResponse<String> response = ApiResponse.success("Password reset successfully", null);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            ApiResponse<String> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception e) {
            ApiResponse<String> response = ApiResponse.error("Failed to reset password");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 