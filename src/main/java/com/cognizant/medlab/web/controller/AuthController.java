package com.cognizant.medlab.web.controller;

import com.cognizant.medlab.application.service.AuthService;
import com.cognizant.medlab.domain.identity.User;
import com.cognizant.medlab.repository.UserRepository;
import com.cognizant.medlab.web.dto.AuthDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * POST /api/auth/login   → returns JWT
 * POST /api/auth/register (ADMIN only)
 * GET  /api/auth/me      → current user info
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, registration, current user")
public class AuthController {

    private final AuthService    authService;
    private final UserRepository userRepository;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT token")
    public ResponseEntity<AuthDto.LoginResponse> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {

        String token = authService.login(request.username(), request.password());

        // Load user directly — avoids @PreAuthorize("ROLE_ADMIN") guard on getAllUsers
        User user = userRepository
            .findByUsernameAndIsDeletedFalse(request.username())
            .orElse(null);

        List<String> roles = (user != null)
            ? user.getAuthorities().stream()
                  .map(a -> a.getAuthority())
                  .collect(Collectors.toList())
            : List.of();

        AuthDto.LoginResponse response = new AuthDto.LoginResponse(
            token,
            request.username(),
            user != null && user.getFullName() != null ? user.getFullName() : request.username(),
            roles,
            expirationMs
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user (ADMIN only)")
    public ResponseEntity<User> register(
            @Valid @RequestBody AuthDto.RegisterUserRequest request) {
        User user = authService.registerUser(
            request.username(), request.email(), request.password(),
            request.fullName(), request.roleName());
        return ResponseEntity.status(201).body(user);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user info")
    public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(java.util.Map.of(
            "id",       user.getId(),
            "username", user.getUsername(),
            "fullName", user.getFullName() != null ? user.getFullName() : "",
            "roles",    user.getAuthorities().stream()
                            .map(a -> a.getAuthority()).collect(Collectors.toList())
        ));
    }
}
