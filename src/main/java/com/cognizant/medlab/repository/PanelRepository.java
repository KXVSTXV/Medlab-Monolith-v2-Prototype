package com.cognizant.medlab.repository;

import com.cognizant.medlab.domain.testcatalog.Panel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PanelRepository extends JpaRepository<Panel, Long> {

    Optional<Panel> findByCodeAndIsDeletedFalse(String code);
    boolean existsByCodeAndIsDeletedFalse(String code);

    @Query("SELECT p FROM Panel p WHERE p.isDeleted = false AND p.active = true")
    Page<Panel> findAllActive(Pageable pageable);
}
