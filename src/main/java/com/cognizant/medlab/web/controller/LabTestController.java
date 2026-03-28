package com.cognizant.medlab.web.controller;

import com.cognizant.medlab.application.service.LabTestService;
import com.cognizant.medlab.domain.testcatalog.*;
import com.cognizant.medlab.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
@Tag(name = "Test Catalog", description = "Lab tests and panel management")
public class LabTestController {

    private final LabTestService labTestService;

    // ── Lab Tests ─────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a lab test (ADMIN, LAB_MANAGER)")
    public ResponseEntity<LabTest> create(
            @Valid @RequestBody OperationsDto.LabTestCreateRequest req) {
        LabTest test = LabTest.builder()
            .code(req.code())
            .name(req.name())
            .description(req.description())
            .specimenType(req.specimenType())
            .turnaroundHours(req.turnaroundHours())
            .price(req.price())
            .referenceRange(req.referenceRange())
            .build();
        return ResponseEntity.status(201).body(labTestService.createTest(test));
    }

    @GetMapping
    @Operation(summary = "List all active lab tests (paged). Use ?search= to filter.")
    public ResponseEntity<PageResponse<LabTest>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        Page<LabTest> result = (search != null && !search.isBlank())
            ? labTestService.searchTests(search, pageable)
            : labTestService.findAllTests(pageable);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lab test by ID")
    public ResponseEntity<LabTest> getById(@PathVariable Long id) {
        return ResponseEntity.ok(labTestService.findTestById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a lab test (ADMIN, LAB_MANAGER)")
    public ResponseEntity<LabTest> update(
            @PathVariable Long id,
            @Valid @RequestBody OperationsDto.LabTestCreateRequest req) {
        LabTest updates = LabTest.builder()
            .name(req.name()).description(req.description())
            .specimenType(req.specimenType()).turnaroundHours(req.turnaroundHours())
            .price(req.price()).referenceRange(req.referenceRange()).build();
        return ResponseEntity.ok(labTestService.updateTest(id, updates));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a lab test (ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        labTestService.deleteTest(id);
        return ResponseEntity.noContent().build();
    }

    // ── Panels ────────────────────────────────────────────────────

    @PostMapping("/panels")
    @Operation(summary = "Create a test panel (ADMIN, LAB_MANAGER)")
    public ResponseEntity<Panel> createPanel(
            @Valid @RequestBody OperationsDto.PanelCreateRequest req) {
        Panel panel = Panel.builder()
            .code(req.code()).name(req.name()).description(req.description()).build();
        return ResponseEntity.status(201)
            .body(labTestService.createPanel(panel, req.testIds()));
    }

    @GetMapping("/panels")
    @Operation(summary = "List all active panels (paged)")
    public ResponseEntity<PageResponse<Panel>> listPanels(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
            labTestService.findAllPanels(PageRequest.of(page, size))));
    }

    @GetMapping("/panels/{id}")
    @Operation(summary = "Get panel by ID")
    public ResponseEntity<Panel> getPanel(@PathVariable Long id) {
        return ResponseEntity.ok(labTestService.findPanelById(id));
    }
}
