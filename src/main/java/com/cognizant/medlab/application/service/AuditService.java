package com.cognizant.medlab.application.service;

import com.cognizant.medlab.domain.audit.AuditLog;
import com.cognizant.medlab.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuditService — writes immutable AuditLog records for every critical action.
 *
 * Runs in a separate transaction (REQUIRES_NEW) so audit writes succeed even
 * if the parent transaction rolls back.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entity, Long entityId, String details) {
        String actor = currentActor();
        AuditLog entry = AuditLog.builder()
            .actor(actor)
            .action(action)
            .entity(entity)
            .entityId(entityId)
            .details(details)
            .build();
        auditLogRepository.save(entry);
        log.debug("[AUDIT] {} → {} #{}: {}", actor, action, entityId, details);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByActor(String actor, Pageable pageable) {
        return auditLogRepository.findByActor(actor, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByEntity(String entity, Pageable pageable) {
        return auditLogRepository.findByEntity(entity, pageable);
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "SYSTEM";
    }
}
