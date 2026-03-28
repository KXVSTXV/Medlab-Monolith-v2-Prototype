package com.cognizant.medlab.domain.testcatalog;

import com.cognizant.medlab.domain.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Lab Test catalog entry.
 *
 * Named "LabTest" to avoid collision with JUnit's @Test annotation.
 * Equivalent to the "Test" entity in the domain model document.
 */
@Entity
@Table(name = "lab_tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = false, of = "code")
public class LabTest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false, length = 50)
    private String code;        // e.g. "CBC", "COVID-PCR"

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;        // e.g. "Complete Blood Count"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "specimen_type", length = 50)
    private String specimenType;  // BLOOD, URINE, etc.

    @Column(name = "turnaround_hours")
    private Integer turnaroundHours;

    @DecimalMin("0.0")
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "reference_range", length = 200)
    private String referenceRange;  // e.g. "70-100 mg/dL"

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
