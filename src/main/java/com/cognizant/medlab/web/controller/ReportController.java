package com.cognizant.medlab.web.controller;

import com.cognizant.medlab.application.service.ReportService;
import com.cognizant.medlab.domain.reporting.Report;
import com.cognizant.medlab.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/**
 * Report Service REST endpoints — mandatory core service (Project 10).
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Lab report generation, verification and release")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/appointment/{appointmentId}/generate")
    @Operation(summary = "Generate consolidated DRAFT report for an appointment")
    public ResponseEntity<Report> generate(@PathVariable Long appointmentId) {
        return ResponseEntity.status(201).body(reportService.generateReport(appointmentId));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify a DRAFT report (LAB_MANAGER, DOCTOR, ADMIN)")
    public ResponseEntity<Report> verify(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.verifyReport(id));
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release a VERIFIED report — notifies patient (LAB_MANAGER, ADMIN)")
    public ResponseEntity<Report> release(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.releaseReport(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get report by ID")
    public ResponseEntity<Report> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.findById(id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download report summary as plain text")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Report report = reportService.findById(id);
        byte[] content = (report.getSummary() != null
            ? report.getSummary()
            : "No summary available.").getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + report.getReportNumber() + ".txt\"")
            .body(content);
    }

    @GetMapping
    @Operation(summary = "List all reports (paged)")
    public ResponseEntity<PageResponse<Report>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
            reportService.findAll(PageRequest.of(page, size))));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "List reports by patient")
    public ResponseEntity<PageResponse<Report>> byPatient(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
            reportService.findByPatient(patientId, PageRequest.of(page, size))));
    }
}
