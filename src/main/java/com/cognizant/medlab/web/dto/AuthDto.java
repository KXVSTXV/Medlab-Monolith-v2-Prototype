package com.cognizant.medlab.web.dto;

import jakarta.validation.constraints.*;

/** Request/response DTOs for the Auth API. */
public final class AuthDto {

    private AuthDto() {}

    public record LoginRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password
    ) {}

    public record LoginResponse(
        String token,
        String username,
        String fullName,
        java.util.List<String> roles,
        long expiresInMs
    ) {}

    public record RegisterUserRequest(
        @NotBlank String username,
        @Email @NotBlank String email,
        @NotBlank String password,
        String fullName,
        @NotBlank String roleName
    ) {}
}
