package com.cognizant.medlab.repository;

import com.cognizant.medlab.domain.processing.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    Optional<TestResult> findByTestOrderIdAndIsDeletedFalse(Long testOrderId);

    @Query("""
        SELECT r FROM TestResult r
        WHERE r.testOrder.sample.appointment.id = :aid AND r.isDeleted = false
        """)
    List<TestResult> findByAppointmentId(@Param("aid") Long appointmentId);
}
