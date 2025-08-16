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
     * Returns the current authenticated user's profile.
     * Requires Authorization: Bearer <access-token>.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Principal principal) {
        try {
            if (principal == null || principal.getName() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
            }
            String username = principal.getName();
            User user = userRepository.findByUsername(username)
                    .orElseGet(() -> userRepository.findByEmail(username).orElse(null));
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("User not found"));
            }
            return ResponseEntity.ok(ApiResponse.success("OK", UserResponse.fromUser(user)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to fetch profile"));
        }
    }

    /**
     * JWT-based login endpoint. Returns access token (short-lived).
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtLoginResponse>> login(@Valid @RequestBody LoginRequest request, BindingResult bindingResult) {
        try {
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.joining(", "));
                return ResponseEntity.badRequest().body(ApiResponse.error(errorMessage));
            }
            JwtLoginResponse resp = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Login successful", resp));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Login failed"));
        }
    }

    /**
     * Renews an about-to-expire token by issuing a new JWT. Requires Authorization header with current token.
     */
    @PostMapping("/token/renew")
    public ResponseEntity<ApiResponse<JwtLoginResponse>> renew(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        try {
            JwtLoginResponse resp = authService.renew(authorization);
            return ResponseEntity.ok(ApiResponse.success("Token renewed", resp));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Renew failed"));
        }
    }

    /**
     * Logs out by revoking the current JWT so it cannot be used anymore.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        try {
            authService.logout(authorization);
            return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Logout failed"));
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