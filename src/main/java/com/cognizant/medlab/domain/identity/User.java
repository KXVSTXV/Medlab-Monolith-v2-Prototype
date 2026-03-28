package com.cognizant.medlab.domain.identity;

import com.cognizant.medlab.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * System user — authentication, authorisation, and audit principal.
 *
 * Implements Spring Security UserDetails so it can be used directly
 * with CustomUserDetailsService without an adapter layer.
 *
 * Upgrade path v1→v2: replaces manual AuthContext thread-local session.
 * Upgrade path v2→v3: extract to Identity micro-service + Keycloak/OAuth2.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"passwordHash", "roles"})
@EqualsAndHashCode(callSuper = false, of = "username")
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // ── UserDetails ───────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                    .map(r -> new SimpleGrantedAuthority(r.getName()))
                    .collect(Collectors.toSet());
    }

    @Override public String  getPassword()              { return passwordHash; }
    @Override public boolean isAccountNonExpired()      { return status != UserStatus.EXPIRED; }
    @Override public boolean isAccountNonLocked()       { return status != UserStatus.LOCKED; }
    @Override public boolean isCredentialsNonExpired()  { return true; }
    @Override public boolean isEnabled()                { return status == UserStatus.ACTIVE && !isDeleted(); }

    // ── Convenience helpers ───────────────────────────────────────

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getName().equals(roleName));
    }

    // ── Status enum ───────────────────────────────────────────────

    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        LOCKED,
        EXPIRED
    }
}
