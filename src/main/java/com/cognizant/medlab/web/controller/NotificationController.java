package com.cognizant.medlab.web.controller;

import com.cognizant.medlab.application.service.NotificationService;
import com.cognizant.medlab.domain.notification.NotificationLog;
import com.cognizant.medlab.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Notification Service REST endpoints — mandatory core service (Project 10).
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification log management")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List all notifications (paged, ADMIN/LAB_MANAGER)")
    public ResponseEntity<PageResponse<NotificationLog>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
            notificationService.getAll(PageRequest.of(page, size))));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "List notifications for a patient")
    public ResponseEntity<List<NotificationLog>> byPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(notificationService.getForPatient(patientId));
    }

    @GetMapping("/patient/{patientId}/unread")
    @Operation(summary = "List unread notifications for a patient")
    public ResponseEntity<List<NotificationLog>> unread(@PathVariable Long patientId) {
        return ResponseEntity.ok(notificationService.getUnreadForPatient(patientId));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }
}
