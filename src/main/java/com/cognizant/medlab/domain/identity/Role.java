package com.cognizant.medlab.domain.identity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Role entity — maps to Spring Security GrantedAuthority names.
 *
 * Seeded by DevDataLoader / Flyway V2 seed script.
 * Roles do NOT extend BaseEntity (no soft-delete needed; roles are reference data).
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "name")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Spring Security authority name, e.g. "ROLE_ADMIN". */
    @Column(unique = true, nullable = false, length = 50)
    private String name;

    // ── Well-known role name constants ────────────────────────────
    public static final String ADMIN       = "ROLE_ADMIN";
    public static final String LAB_MANAGER = "ROLE_LAB_MANAGER";
    public static final String LAB_TECH    = "ROLE_LAB_TECH";
    public static final String DOCTOR      = "ROLE_DOCTOR";
    public static final String RECEPTION   = "ROLE_RECEPTION";
    public static final String BILLING     = "ROLE_BILLING";
}
