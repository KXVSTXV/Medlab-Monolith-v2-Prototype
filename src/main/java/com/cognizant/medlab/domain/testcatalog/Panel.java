package com.cognizant.medlab.domain.testcatalog;

import com.cognizant.medlab.domain.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Panel — a named bundle of lab tests ordered together.
 * Example: "Basic Metabolic Panel" includes CMP tests.
 */
@Entity
@Table(name = "panels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "tests")
@EqualsAndHashCode(callSuper = false, of = "code")
public class Panel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "panel_tests",
        joinColumns        = @JoinColumn(name = "panel_id"),
        inverseJoinColumns = @JoinColumn(name = "lab_test_id")
    )
    @Builder.Default
    private Set<LabTest> tests = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
