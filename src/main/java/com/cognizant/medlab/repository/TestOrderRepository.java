package com.cognizant.medlab.repository;

import com.cognizant.medlab.domain.processing.TestOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestOrderRepository extends JpaRepository<TestOrder, Long> {

    @EntityGraph(attributePaths = {"sample.appointment.patient", "labTest"})
    @Query("SELECT o FROM TestOrder o WHERE o.isDeleted = false")
    Page<TestOrder> findAllActive(Pageable pageable);

    @Query("SELECT o FROM TestOrder o WHERE o.sample.id = :sid AND o.isDeleted = false")
    List<TestOrder> findBySampleId(@Param("sid") Long sampleId);

    @Query("""
        SELECT o FROM TestOrder o
        WHERE o.sample.appointment.patient.id = :pid AND o.isDeleted = false
        """)
    Page<TestOrder> findByPatientId(@Param("pid") Long patientId, Pageable pageable);

    @Query("SELECT o FROM TestOrder o WHERE o.status = :status AND o.isDeleted = false")
    Page<TestOrder> findByStatus(
            @Param("status") TestOrder.TestOrderStatus status, Pageable pageable);
}
