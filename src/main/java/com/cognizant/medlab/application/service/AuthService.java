package com.cognizant.medlab.application.service;

import com.cognizant.medlab.domain.identity.Role;
import com.cognizant.medlab.domain.identity.User;
import com.cognizant.medlab.exception.*;
import com.cognizant.medlab.repository.*;
import com.cognizant.medlab.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Authentication & User-management service.
 *
 * Upgrade path v1→v2:
 *   - AuthContext thread-local → Spring Security + JWT.
 *   - SHA-256 PasswordUtil → BCrypt via PasswordEncoder.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository        userRepository;
    private final RoleRepository        roleRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtUtil               jwtUtil;
    private final AuditService          auditService;

    // ── Login ────────────────────────────────────────────────────

    /**
     * Authenticates credentials and returns a signed JWT.
     *
     * @throws BadCredentialsException   on wrong password
     * @throws DisabledException         on inactive account
     */
    @Transactional(readOnly = true)
    public String login(String username, String password) {
        // Delegate credential check to Spring Security
        var auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password));

        UserDetails user = (UserDetails) auth.getPrincipal();
        String token = jwtUtil.generateToken(user);
        log.info("User '{}' logged in successfully", username);
        auditService.log("USER_LOGIN", "User", null, "username=" + username);
        return token;
    }

    // ── User registration ─────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public User registerUser(String username, String email, String password,
                             String fullName, String roleName) {
        if (userRepository.existsByUsernameAndIsDeletedFalse(username))
            throw new DuplicateResourceException("User", "username", username);
        if (userRepository.existsByEmailAndIsDeletedFalse(email))
            throw new DuplicateResourceException("User", "email", email);

        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new ResourceNotFoundException("Role", roleName));

        User user = User.builder()
            .username(username)
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .fullName(fullName)
            .status(User.UserStatus.ACTIVE)
            .roles(Set.of(role))
            .build();

        User saved = userRepository.save(user);
        auditService.log("USER_REGISTERED", "User", saved.getId(),
                         "username=" + username + ", role=" + roleName);
        log.info("User '{}' registered with role '{}'", username, roleName);
        return saved;
    }

    // ── User management ───────────────────────────────────────────

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAllActive(pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .filter(u -> !u.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void softDeleteUser(Long id) {
        User user = getUserById(id);
        user.setDeleted(true);
        user.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(user);
        auditService.log("USER_DELETED", "User", id, "username=" + user.getUsername());
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public User updateUserStatus(Long id, User.UserStatus status) {
        User user = getUserById(id);
        user.setStatus(status);
        User saved = userRepository.save(user);
        auditService.log("USER_STATUS_CHANGED", "User", id, "status=" + status);
        return saved;
    }
}
