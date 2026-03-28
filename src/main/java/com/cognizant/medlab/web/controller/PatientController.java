package com.cognizant.medlab.web.controller;

import com.cognizant.medlab.application.service.PatientService;
import com.cognizant.medlab.domain.patient.Patient;
import com.cognizant.medlab.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST API for Patient Management.
 *
 * All list endpoints support:
 *   ?page=0&size=20&sort=fullName,asc
 * Search:
 *   GET /api/patients?search=john
 */
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patient lifecycle and KYC management")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @Operation(summary = "Register a new patient (ADMIN, RECEPTION)")
    public ResponseEntity<PatientDto.Response> register(
            @Valid @RequestBody PatientDto.CreateRequest req) {

        Patient patient = Patient.builder()
            .fullName(req.fullName())
            .dob(req.dob())
            .gender(req.gender())
            .email(req.email())
            .mobileNumber(req.mobileNumber())
            .address(req.address())
            .bloodGroup(req.bloodGroup())
            .allergies(req.allergies())
            .medicalHistory(req.medicalHistory())
            .consentGiven(req.consentGiven())
            .build();

        return ResponseEntity.status(201)
            .body(PatientDto.Response.from(patientService.register(patient)));
    }

    @GetMapping
    @Operation(summary = "List all patients (paged). Use ?search=query to filter.")
    public ResponseEntity<PageResponse<PatientDto.Response>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName,asc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.fromString(sort.length > 1 ? sort[1] : "asc"), sort[0]));

        Page<Patient> result = (search != null && !search.isBlank())
            ? patientService.search(search, pageable)
            : patientService.findAll(pageable);

        return ResponseEntity.ok(PageResponse.from(result.map(PatientDto.Response::from)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID")
    public ResponseEntity<PatientDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(PatientDto.Response.from(patientService.findById(id)));
    }

    @GetMapping("/mrn/{mrn}")
    @Operation(summary = "Get patient by Medical Record Number")
    public ResponseEntity<PatientDto.Response> getByMrn(@PathVariable String mrn) {
        return ResponseEntity.ok(PatientDto.Response.from(patientService.findByMrn(mrn)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient demographics (ADMIN, RECEPTION)")
    public ResponseEntity<PatientDto.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody PatientDto.UpdateRequest req) {

        Patient updates = Patient.builder()
            .fullName(req.fullName())
            .dob(req.dob())
            .gender(req.gender())
            .address(req.address())
            .bloodGroup(req.bloodGroup())
            .allergies(req.allergies())
            .medicalHistory(req.medicalHistory())
            .consentGiven(req.consentGiven())
            .build();

        return ResponseEntity.ok(PatientDto.Response.from(patientService.update(id, updates)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a patient (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        patientService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
