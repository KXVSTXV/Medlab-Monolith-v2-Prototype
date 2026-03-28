package com.cognizant.medlab.domain.reporting;

import com.cognizant.medlab.domain.common.BaseEntity;
import com.cognizant.medlab.domain.patient.Patient;
import com.cognizant.medlab.domain.scheduling.Appointment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Report — consolidated lab report for a patient visit.
 *
 * Aggregates all TestResults for an Appointment.
 * Status lifecycle: DRAFT → VERIFIED → RELEASED
 *
 * pdfRef stores the path/key to the generated PDF (local file or S3 key).
 * For v2, a text-based PDF is generated; iText can replace it later.
 */
@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"patient", "appointment"})
@EqualsAndHashCode(callSuper = false, of = "reportNumber")
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_number", unique = true, nullable = false, length = 100)
    private String reportNumber;        // e.g. "RPT-2026-000042"

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "pdf_ref", length = 500)
    private String pdfRef;              // file path or S3 key

    @Column(name = "has_abnormal", nullable = false)
    @Builder.Default
    private boolean hasAbnormal = false;

    @Column(name = "prepared_by", length = 100)
    private String preparedBy;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String summary;

    // ── Enum ─────────────────────────────────────────────────────

    public enum ReportStatus {
        DRAFT,
        VERIFIED,
        RELEASED
    }
}
