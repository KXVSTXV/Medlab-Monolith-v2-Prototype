package com.cognizant.medlab.repository;

import com.cognizant.medlab.domain.testcatalog.LabTest;
import com.cognizant.medlab.domain.testcatalog.Panel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LabTestRepository extends JpaRepository<LabTest, Long> {

    Optional<LabTest> findByCodeAndIsDeletedFalse(String code);
    boolean existsByCodeAndIsDeletedFalse(String code);

    @Query("SELECT t FROM LabTest t WHERE t.isDeleted = false AND t.active = true")
    Page<LabTest> findAllActive(Pageable pageable);

    @Query("""
        SELECT t FROM LabTest t
        WHERE t.isDeleted = false AND t.active = true
          AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(t.code) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<LabTest> search(@Param("q") String query, Pageable pageable);
}
