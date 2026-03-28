package com.cognizant.medlab.domain.audit;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * AuditLog — immutable write-only record of critical system actions.
 *
 * NOT extending BaseEntity intentionally:
 *  - Audit logs are never soft-deleted.
 *  - They have only created_at (no updated_at, no created_by foreign key).
 *  - They store the actor as a plain string for immutability.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String actor;           // username who performed the action

    @Column(nullable = false, length = 100)
    private String action;          // e.g. "PATIENT_REGISTERED", "REPORT_RELEASED"

    @Column(nullable = false, length = 100)
    private String entity;          // e.g. "Patient", "Report"

    @Column(name = "entity_id")
    private Long entityId;

    @Column(columnDefinition = "TEXT")
    private String details;         // JSON or human-readable context

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
