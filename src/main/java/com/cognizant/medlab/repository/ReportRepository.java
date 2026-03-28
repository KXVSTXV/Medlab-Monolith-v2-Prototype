package com.cognizant.medlab.repository;

import com.cognizant.medlab.domain.reporting.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.lang.NonNull;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByReportNumberAndIsDeletedFalse(String reportNumber);
    Optional<Report> findByAppointmentIdAndIsDeletedFalse(Long appointmentId);

    @NonNull
    @EntityGraph(attributePaths = {"patient"})
    Optional<Report> findById(@NonNull Long id);

    @EntityGraph(attributePaths = {"patient"})
    @Query("SELECT r FROM Report r WHERE r.isDeleted = false")
    Page<Report> findAllActive(Pageable pageable);

    @EntityGraph(attributePaths = {"patient"})
    @Query("SELECT r FROM Report r WHERE r.patient.id = :pid AND r.isDeleted = false")
    Page<Report> findByPatientId(@Param("pid") Long patientId, Pageable pageable);
}
