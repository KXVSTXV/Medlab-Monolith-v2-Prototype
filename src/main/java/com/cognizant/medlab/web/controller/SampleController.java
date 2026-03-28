package com.cognizant.medlab.web.controller;

import com.cognizant.medlab.application.service.SampleService;
import com.cognizant.medlab.domain.scheduling.Sample;
import com.cognizant.medlab.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Sample Service REST endpoints — mandatory core service (Project 10).
 */
@RestController
@RequestMapping("/api/samples")
@RequiredArgsConstructor
@Tag(name = "Samples", description = "Sample collection and processing status")
public class SampleController {

    private final SampleService sampleService;

    @PostMapping
    @Operation(summary = "Collect a new sample (LAB_TECH, LAB_MANAGER, ADMIN)")
    public ResponseEntity<Sample> collect(
            @Valid @RequestBody OperationsDto.SampleCollectRequest req) {
        return ResponseEntity.status(201).body(
            sampleService.collectSample(req.appointmentId(), req.specimenType()));
    }

    @GetMapping
    @Operation(summary = "List all samples (paged)")
    public ResponseEntity<PageResponse<Sample>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
            sampleService.findAll(PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sample by ID")
    public ResponseEntity<Sample> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sampleService.findById(id));
    }

    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "Get sample by barcode")
    public ResponseEntity<Sample> getByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(sampleService.findByBarcode(barcode));
    }

    @GetMapping("/appointment/{appointmentId}")
    @Operation(summary = "List samples by appointment")
    public ResponseEntity<PageResponse<Sample>> byAppointment(
            @PathVariable Long appointmentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
            sampleService.findByAppointment(appointmentId, PageRequest.of(page, size))));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update sample status (LAB_TECH, LAB_MANAGER, ADMIN)")
    public ResponseEntity<Sample> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OperationsDto.SampleStatusRequest req) {
        return ResponseEntity.ok(
            sampleService.updateStatus(id, req.status(), req.rejectionReason()));
    }
}
