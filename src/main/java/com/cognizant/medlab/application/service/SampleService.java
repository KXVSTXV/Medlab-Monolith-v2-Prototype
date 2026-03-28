package com.cognizant.medlab.application.service;

import com.cognizant.medlab.domain.notification.NotificationLog.NotificationType;
import com.cognizant.medlab.domain.scheduling.*;
import com.cognizant.medlab.exception.*;
import com.cognizant.medlab.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Sample Service — mandatory core service (Project 10).
 *
 * Handles biological specimen collection, barcode generation,
 * and status transitions.
 *
 * Lifecycle: COLLECTED → PROCESSING → ANALYSED | REJECTED
 *
 * Upgrade path v1→v2:
 *   - JDBC DAO replaced by JpaRepository.
 *   - Console notification replaced by NotificationService async call.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SampleService {

    private final SampleRepository      sampleRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationService   notificationService;
    private final AuditService          auditService;

    // ── Collect sample ────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_LAB_TECH','ROLE_LAB_MANAGER')")
    public Sample collectSample(Long appointmentId, Sample.SpecimenType specimenType) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .filter(a -> !a.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        if (appointment.getStatus() == Appointment.AppointmentStatus.CANCELLED)
            throw new BusinessRuleException(
                "Cannot collect sample for a CANCELLED appointment.");

        String barcode = generateBarcode(specimenType);
        String collector = SecurityContextHolder.getContext().getAuthentication().getName();

        Sample sample = Sample.builder()
            .appointment(appointment)
            .barcode(barcode)
            .specimenType(specimenType)
            .collectedAt(LocalDateTime.now())
            .collectedBy(collector)
            .status(Sample.SampleStatus.COLLECTED)
            .build();

        Sample saved = sampleRepository.save(sample);

        // Advance appointment status
        appointment.setStatus(Appointment.AppointmentStatus.SAMPLE_COLLECTED);
        appointmentRepository.save(appointment);

        // Notify patient
        notificationService.send(
            appointment.getPatient().getId(),
            "Your " + specimenType + " sample (barcode: " + barcode
                + ") has been collected for appointment #" + appointmentId + ".",
            NotificationType.SAMPLE_COLLECTED
        );

        auditService.log("SAMPLE_COLLECTED", "Sample", saved.getId(),
                         "barcode=" + barcode + ", appointment=" + appointmentId);
        log.info("Sample collected: barcode={}, appointment={}", barcode, appointmentId);
        return saved;
    }

    // ── Update status ─────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_LAB_TECH','ROLE_LAB_MANAGER')")
    public Sample updateStatus(Long sampleId, Sample.SampleStatus newStatus,
                               String rejectionReason) {
        Sample sample = findById(sampleId);

        // Validate transitions
        validateTransition(sample.getStatus(), newStatus);

        sample.setStatus(newStatus);
        if (newStatus == Sample.SampleStatus.REJECTED && rejectionReason != null) {
            sample.setRejectionReason(rejectionReason);
        }

        Sample saved = sampleRepository.save(sample);
        auditService.log("SAMPLE_STATUS_CHANGED", "Sample", sampleId,
                         "status=" + newStatus);
        return saved;
    }

    // ── Query ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Sample findById(Long id) {
        return sampleRepository.findById(id)
            .filter(s -> !s.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Sample", id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Sample findByBarcode(String barcode) {
        return sampleRepository.findByBarcodeAndIsDeletedFalse(barcode)
            .orElseThrow(() -> new ResourceNotFoundException("Sample with barcode " + barcode));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Sample> findAll(Pageable pageable) {
        return sampleRepository.findAllActive(pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Sample> findByAppointment(Long appointmentId, Pageable pageable) {
        return sampleRepository.findByAppointmentId(appointmentId, pageable);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String generateBarcode(Sample.SpecimenType type) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String uid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return type.name().substring(0, 2) + "-" + ts + "-" + uid;
    }

    private void validateTransition(Sample.SampleStatus current, Sample.SampleStatus next) {
        boolean valid = switch (current) {
            case COLLECTED  -> next == Sample.SampleStatus.PROCESSING
                            || next == Sample.SampleStatus.REJECTED;
            case PROCESSING -> next == Sample.SampleStatus.ANALYSED
                            || next == Sample.SampleStatus.REJECTED;
            default         -> false;
        };
        if (!valid)
            throw new BusinessRuleException(
                "Invalid sample status transition: " + current + " → " + next);
    }
}
