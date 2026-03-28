package com.cognizant.medlab.application.service;

import com.cognizant.medlab.domain.patient.Patient;
import com.cognizant.medlab.exception.*;
import com.cognizant.medlab.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Patient management service.
 *
 * MRN format: MRN-{zero-padded-id}, e.g. MRN-000042.
 * Consent must be given before scheduling appointments (enforced by AppointmentService).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final AuditService      auditService;

    // ── Create ───────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPTION')")
    public Patient register(Patient patient) {
        if (patient.getEmail() != null
                && patientRepository.existsByEmailAndIsDeletedFalse(patient.getEmail()))
            throw new DuplicateResourceException("Patient", "email", patient.getEmail());

        if (patient.getMobileNumber() != null
                && patientRepository.existsByMobileNumberAndIsDeletedFalse(
                        patient.getMobileNumber()))
            throw new DuplicateResourceException("Patient", "mobileNumber",
                    patient.getMobileNumber());

        // Save first to get the generated ID, then set MRN
        Patient saved = patientRepository.save(patient);
        saved.setMedicalRecordNumber(String.format("MRN-%06d", saved.getId()));
        saved = patientRepository.save(saved);

        auditService.log("PATIENT_REGISTERED", "Patient", saved.getId(),
                         "mrn=" + saved.getMedicalRecordNumber()
                         + ", name=" + saved.getFullName());
        log.info("Patient registered: {}", saved.getMedicalRecordNumber());
        return saved;
    }

    // ── Read ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Patient findById(Long id) {
        return patientRepository.findById(id)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Patient", id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Patient findByMrn(String mrn) {
        return patientRepository.findByMedicalRecordNumberAndIsDeletedFalse(mrn)
            .orElseThrow(() -> new ResourceNotFoundException("Patient with MRN " + mrn));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Patient> findAll(Pageable pageable) {
        return patientRepository.findAllActive(pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Patient> search(String query, Pageable pageable) {
        return patientRepository.search(query, pageable);
    }

    // ── Update ───────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPTION')")
    public Patient update(Long id, Patient updates) {
        Patient existing = findById(id);
        existing.setFullName(updates.getFullName());
        existing.setDob(updates.getDob());
        existing.setGender(updates.getGender());
        existing.setAddress(updates.getAddress());
        existing.setBloodGroup(updates.getBloodGroup());
        existing.setAllergies(updates.getAllergies());
        existing.setMedicalHistory(updates.getMedicalHistory());
        existing.setConsentGiven(updates.isConsentGiven());

        Patient saved = patientRepository.save(existing);
        auditService.log("PATIENT_UPDATED", "Patient", id, "mrn=" + existing.getMedicalRecordNumber());
        return saved;
    }

    // ── Soft delete ───────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void softDelete(Long id) {
        Patient patient = findById(id);
        patient.setDeleted(true);
        patient.setStatus(Patient.PatientStatus.INACTIVE);
        patientRepository.save(patient);
        auditService.log("PATIENT_DELETED", "Patient", id,
                         "mrn=" + patient.getMedicalRecordNumber());
        log.info("Patient soft-deleted: {}", patient.getMedicalRecordNumber());
    }
}
