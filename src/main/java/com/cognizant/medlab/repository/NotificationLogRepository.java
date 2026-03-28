package com.cognizant.medlab.repository;

import com.cognizant.medlab.domain.notification.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    @Query("SELECT n FROM NotificationLog n WHERE n.patientId = :pid AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<NotificationLog> findByPatientId(@Param("pid") Long patientId);

    @Query("SELECT n FROM NotificationLog n WHERE n.patientId = :pid AND n.read = false AND n.isDeleted = false")
    List<NotificationLog> findUnreadByPatientId(@Param("pid") Long patientId);

    @Query("SELECT n FROM NotificationLog n WHERE n.isDeleted = false")
    Page<NotificationLog> findAllActive(Pageable pageable);
}
