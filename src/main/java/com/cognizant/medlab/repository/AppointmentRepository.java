package com.cognizant.medlab.repository;

import com.cognizant.medlab.domain.scheduling.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = {"patient"})
    @Query("SELECT a FROM Appointment a WHERE a.isDeleted = false")
    Page<Appointment> findAllActive(Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :pid AND a.isDeleted = false")
    Page<Appointment> findByPatientId(@Param("pid") Long patientId, Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE a.status = :status AND a.isDeleted = false")
    Page<Appointment> findByStatus(
            @Param("status") Appointment.AppointmentStatus status, Pageable pageable);
}
