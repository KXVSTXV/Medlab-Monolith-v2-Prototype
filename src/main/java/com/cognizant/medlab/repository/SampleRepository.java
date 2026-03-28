package com.cognizant.medlab.repository;

import com.cognizant.medlab.domain.scheduling.Sample;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SampleRepository extends JpaRepository<Sample, Long> {

    Optional<Sample> findByBarcodeAndIsDeletedFalse(String barcode);

    @Query("SELECT s FROM Sample s WHERE s.isDeleted = false")
    Page<Sample> findAllActive(Pageable pageable);

    @Query("SELECT s FROM Sample s WHERE s.appointment.id = :aid AND s.isDeleted = false")
    Page<Sample> findByAppointmentId(@Param("aid") Long appointmentId, Pageable pageable);
}
