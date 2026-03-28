package com.cognizant.medlab.repository;

import com.cognizant.medlab.domain.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByActor(String actor, Pageable pageable);

    Page<AuditLog> findByEntity(String entity, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.entity = :entity AND a.entityId = :id")
    Page<AuditLog> findByEntityAndEntityId(
            @Param("entity") String entity,
            @Param("id") Long entityId,
            Pageable pageable);
}
