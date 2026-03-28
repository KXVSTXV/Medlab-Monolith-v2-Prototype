package com.cognizant.medlab.web.controller;

import com.cognizant.medlab.application.service.AppointmentService;
import com.cognizant.medlab.domain.scheduling.Appointment;
import com.cognizant.medlab.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Patient appointment scheduling")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Schedule a new appointment")
    public ResponseEntity<Appointment> schedule(
            @Valid @RequestBody OperationsDto.AppointmentCreateRequest req) {
        return ResponseEntity.status(201).body(
            appointmentService.schedule(req.patientId(), req.scheduledAt(),
                                        req.location(), req.notes()));
    }

    @GetMapping
    @Operation(summary = "List all appointments (paged)")
    public ResponseEntity<PageResponse<Appointment>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "scheduledAt,desc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.fromString(sort.length > 1 ? sort[1] : "desc"), sort[0]));
        return ResponseEntity.ok(PageResponse.from(appointmentService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<Appointment> getById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.findById(id));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "List appointments by patient")
    public ResponseEntity<PageResponse<Appointment>> byPatient(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
            appointmentService.findByPatient(patientId, PageRequest.of(page, size))));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update appointment status")
    public ResponseEntity<Appointment> updateStatus(
            @PathVariable Long id,
            @RequestParam Appointment.AppointmentStatus status) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel appointment")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        appointmentService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
