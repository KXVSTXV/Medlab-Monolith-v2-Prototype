package com.cognizant.medlab.web.controller;

import com.cognizant.medlab.application.service.*;
import com.cognizant.medlab.domain.audit.AuditLog;
import com.cognizant.medlab.domain.identity.User;
import com.cognizant.medlab.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Admin", description = "Admin-only: audit logs, user management")
public class AdminController {

    private final AuditService auditService;
    private final AuthService  authService;

    // ── Audit logs ────────────────────────────────────────────────

    @GetMapping("/auditlogs")
    @Operation(summary = "List all audit log entries (paged)")
    public ResponseEntity<PageResponse<AuditLog>> auditLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.fromString(sort.length > 1 ? sort[1] : "desc"), sort[0]));
        return ResponseEntity.ok(PageResponse.from(auditService.findAll(pageable)));
    }

    @GetMapping("/auditlogs/actor/{actor}")
    @Operation(summary = "Audit log entries by actor (username)")
    public ResponseEntity<PageResponse<AuditLog>> byActor(
            @PathVariable String actor,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
            auditService.findByActor(actor, PageRequest.of(page, size))));
    }

    @GetMapping("/auditlogs/entity/{entity}")
    @Operation(summary = "Audit log entries by entity type (e.g. Patient, Report)")
    public ResponseEntity<PageResponse<AuditLog>> byEntity(
            @PathVariable String entity,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
            auditService.findByEntity(entity, PageRequest.of(page, size))));
    }

    // ── User management ───────────────────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "List all users (paged)")
    public ResponseEntity<PageResponse<User>> users(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
            authService.getAllUsers(PageRequest.of(page, size))));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Soft-delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        authService.softDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Update user account status (ACTIVE/LOCKED/INACTIVE)")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable Long id,
            @RequestParam User.UserStatus status) {
        return ResponseEntity.ok(authService.updateUserStatus(id, status));
    }
}
