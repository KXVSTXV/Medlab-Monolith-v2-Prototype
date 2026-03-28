package com.cognizant.medlab.domain.processing;

import com.cognizant.medlab.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * TestResult — the measured value for a TestOrder.
 *
 * Contains the raw value, unit, reference range, and abnormal flag.
 * The detectAbnormal logic from CLI v1 is carried forward in ReportService.
 */
@Entity
@Table(name = "test_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "testOrder")
@EqualsAndHashCode(callSuper = false, of = "id")
public class TestResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "test_order_id", nullable = false, unique = true)
    private TestOrder testOrder;

    @Column(length = 255)
    private String value;               // e.g. "95", "Negative"

    @Column(length = 50)
    private String unit;                // e.g. "mg/dL"

    @Column(name = "reference_range", length = 200)
    private String referenceRange;      // e.g. "70-100"

    @Column(name = "is_abnormal", nullable = false)
    @Builder.Default
    private boolean abnormal = false;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ResultStatus status = ResultStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    // ── Enum ─────────────────────────────────────────────────────

    public enum ResultStatus {
        PENDING,
        ENTERED,
        VERIFIED
    }
}
