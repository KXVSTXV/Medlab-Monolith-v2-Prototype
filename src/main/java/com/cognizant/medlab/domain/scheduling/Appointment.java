package com.cognizant.medlab.domain.scheduling;

import com.cognizant.medlab.domain.common.BaseEntity;
import com.cognizant.medlab.domain.patient.Patient;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Appointment — a patient visit for test scheduling.
 *
 * Status lifecycle:
 *   SCHEDULED → CONFIRMED → SAMPLE_COLLECTED → COMPLETED | CANCELLED
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "patient")
@EqualsAndHashCode(callSuper = false, of = "id")
public class Appointment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "collector_id")
    private Long collectorId;   // User.id of the assigned sample collector

    @Column(length = 255)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    // ── Enum ─────────────────────────────────────────────────────

    public enum AppointmentStatus {
        SCHEDULED,
        CONFIRMED,
        SAMPLE_COLLECTED,
        COMPLETED,
        CANCELLED
    }
}
