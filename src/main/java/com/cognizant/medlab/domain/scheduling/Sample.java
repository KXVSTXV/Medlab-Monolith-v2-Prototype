package com.cognizant.medlab.domain.scheduling;

import com.cognizant.medlab.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Sample — biological specimen collected at an appointment.
 *
 * Status lifecycle:
 *   COLLECTED → PROCESSING → ANALYSED | REJECTED
 *
 * Upgrade path v1→v2: JDBC DAO replaced by JpaRepository.
 */
@Entity
@Table(name = "samples")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "appointment")
@EqualsAndHashCode(callSuper = false, of = "barcode")
public class Sample extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(unique = true, length = 100)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "specimen_type", nullable = false, length = 20)
    private SpecimenType specimenType;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SampleStatus status = SampleStatus.COLLECTED;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "collected_by", length = 100)
    private String collectedBy;     // username of the collector

    // ── Enums ────────────────────────────────────────────────────

    public enum SpecimenType {
        BLOOD, URINE, STOOL, SWAB, SPUTUM, CSF, OTHER
    }

    public enum SampleStatus {
        COLLECTED, PROCESSING, ANALYSED, REJECTED
    }
}
