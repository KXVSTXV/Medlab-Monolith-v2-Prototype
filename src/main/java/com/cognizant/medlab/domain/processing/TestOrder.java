package com.cognizant.medlab.domain.processing;

import com.cognizant.medlab.domain.common.BaseEntity;
import com.cognizant.medlab.domain.scheduling.Sample;
import com.cognizant.medlab.domain.testcatalog.LabTest;
import jakarta.persistence.*;
import lombok.*;

/**
 * TestOrder — work item linking a Sample to a specific LabTest.
 *
 * Status lifecycle:
 *   ORDERED → SAMPLE_COLLECTED → IN_PROGRESS → RESULT_ENTERED → VERIFIED → COMPLETED
 *                                                                           → CANCELLED
 *
 * Carries over from CLI v1 with priority tiers.
 */
@Entity
@Table(name = "test_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"sample", "labTest"})
@EqualsAndHashCode(callSuper = false, of = "id")
public class TestOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id", nullable = false)
    private Sample sample;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_test_id", nullable = false)
    private LabTest labTest;

    @Column(name = "ordered_by", length = 100)
    private String orderedBy;   // doctor username

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TestOrderStatus status = TestOrderStatus.ORDERED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.ROUTINE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ── Enums ────────────────────────────────────────────────────

    public enum TestOrderStatus {
        ORDERED,
        SAMPLE_COLLECTED,
        IN_PROGRESS,
        RESULT_ENTERED,
        VERIFIED,
        COMPLETED,
        CANCELLED
    }

    public enum Priority {
        ROUTINE,
        URGENT,
        STAT
    }
}
