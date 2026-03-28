package com.cognizant.medlab.web.controller;

import com.cognizant.medlab.application.service.TestOrderService;
import com.cognizant.medlab.domain.processing.*;
import com.cognizant.medlab.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Test Order Service REST endpoints — mandatory core service (Project 10).
 */
@RestController
@RequestMapping("/api/testorders")
@RequiredArgsConstructor
@Tag(name = "Test Orders", description = "Lab test order lifecycle and result management")
public class TestOrderController {

    private final TestOrderService testOrderService;

    @PostMapping
    @Operation(summary = "Create a test order (ADMIN, RECEPTION, DOCTOR, LAB_MANAGER)")
    public ResponseEntity<TestOrder> create(
            @Valid @RequestBody OperationsDto.TestOrderCreateRequest req) {
        return ResponseEntity.status(201).body(
            testOrderService.createOrder(
                req.sampleId(), req.labTestId(), req.priority(), req.notes()));
    }

    @GetMapping
    @Operation(summary = "List all test orders (paged)")
    public ResponseEntity<PageResponse<TestOrder>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.fromString(sort.length > 1 ? sort[1] : "desc"), sort[0]));
        return ResponseEntity.ok(PageResponse.from(testOrderService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get test order by ID")
    public ResponseEntity<TestOrder> getById(@PathVariable Long id) {
        return ResponseEntity.ok(testOrderService.findById(id));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "List test orders by patient")
    public ResponseEntity<PageResponse<TestOrder>> byPatient(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
            testOrderService.findByPatient(patientId, PageRequest.of(page, size))));
    }

    @PostMapping("/{id}/results")
    @Operation(summary = "Submit test result (LAB_TECH, LAB_MANAGER, ADMIN)")
    public ResponseEntity<TestResult> submitResult(
            @PathVariable Long id,
            @Valid @RequestBody OperationsDto.TestResultSubmitRequest req) {
        return ResponseEntity.status(201).body(
            testOrderService.submitResult(
                id, req.value(), req.unit(), req.referenceRange(), req.remarks()));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify test result (LAB_TECH, DOCTOR, LAB_MANAGER, ADMIN)")
    public ResponseEntity<TestResult> verifyResult(@PathVariable Long id) {
        return ResponseEntity.ok(testOrderService.verifyResult(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a test order (ADMIN, RECEPTION, LAB_MANAGER)")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        testOrderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}
