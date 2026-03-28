package com.cognizant.medlab.repository;

import com.cognizant.medlab.domain.patient.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByMedicalRecordNumberAndIsDeletedFalse(String mrn);
    Optional<Patient> findByEmailAndIsDeletedFalse(String email);
    Optional<Patient> findByMobileNumberAndIsDeletedFalse(String mobile);
    boolean existsByEmailAndIsDeletedFalse(String email);
    boolean existsByMobileNumberAndIsDeletedFalse(String mobile);

    @Query("SELECT p FROM Patient p WHERE p.isDeleted = false")
    Page<Patient> findAllActive(Pageable pageable);

    @Query("""
        SELECT p FROM Patient p
        WHERE p.isDeleted = false
          AND (LOWER(p.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.email) LIKE LOWER(CONCAT('%', :q, '%'))
            OR p.mobileNumber LIKE CONCAT('%', :q, '%')
            OR p.medicalRecordNumber LIKE CONCAT('%', :q, '%'))
        """)
    Page<Patient> search(@Param("q") String query, Pageable pageable);
}
