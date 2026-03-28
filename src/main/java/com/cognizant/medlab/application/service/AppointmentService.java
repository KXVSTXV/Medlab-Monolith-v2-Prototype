package com.cognizant.medlab.application.service;

import com.cognizant.medlab.domain.notification.NotificationLog.NotificationType;
import com.cognizant.medlab.domain.patient.Patient;
import com.cognizant.medlab.domain.scheduling.Appointment;
import com.cognizant.medlab.exception.*;
import com.cognizant.medlab.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Appointment scheduling service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository     patientRepository;
    private final NotificationService   notificationService;
    private final AuditService          auditService;

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPTION','ROLE_LAB_MANAGER')")
    public Appointment schedule(Long patientId, LocalDateTime scheduledAt,
                                String location, String notes) {
        Patient patient = patientRepository.findById(patientId)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        if (!patient.isConsentGiven())
            throw new BusinessRuleException(
                "Patient #" + patientId + " has not given consent. Update consent before scheduling.");

        Appointment appointment = Appointment.builder()
            .patient(patient)
            .scheduledAt(scheduledAt)
            .location(location)
            .notes(notes)
            .status(Appointment.AppointmentStatus.SCHEDULED)
            .build();

        Appointment saved = appointmentRepository.save(appointment);

        notificationService.send(patientId,
            "Appointment #" + saved.getId() + " scheduled for "
                + scheduledAt + " at " + location,
            NotificationType.APPOINTMENT_SCHEDULED);

        auditService.log("APPOINTMENT_SCHEDULED", "Appointment", saved.getId(),
                         "patientId=" + patientId + ", at=" + scheduledAt);
        return saved;
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPTION','ROLE_LAB_MANAGER')")
    public Appointment updateStatus(Long id, Appointment.AppointmentStatus status) {
        Appointment appointment = findById(id);
        appointment.setStatus(status);
        Appointment saved = appointmentRepository.save(appointment);
        auditService.log("APPOINTMENT_STATUS_CHANGED", "Appointment", id, "status=" + status);
        return saved;
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPTION')")
    public void cancel(Long id) {
        Appointment appointment = findById(id);
        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
        auditService.log("APPOINTMENT_CANCELLED", "Appointment", id, "");
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Appointment findById(Long id) {
        return appointmentRepository.findById(id)
            .filter(a -> !a.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Appointment> findAll(Pageable pageable) {
        return appointmentRepository.findAllActive(pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Appointment> findByPatient(Long patientId, Pageable pageable) {
        return appointmentRepository.findByPatientId(patientId, pageable);
    }
}
