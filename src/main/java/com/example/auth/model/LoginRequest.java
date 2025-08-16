package com.example.auth.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for login API. Either username or email must be provided along with password.
 */
@Data
public class LoginRequest {
    /** Optional if email is provided */
    private String username;
    /** Optional if username is provided */
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
