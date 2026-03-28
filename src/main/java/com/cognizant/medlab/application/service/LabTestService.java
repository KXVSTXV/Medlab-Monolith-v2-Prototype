package com.cognizant.medlab.application.service;

import com.cognizant.medlab.domain.testcatalog.*;
import com.cognizant.medlab.exception.*;
import com.cognizant.medlab.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lab Test Catalog and Panel management service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabTestService {

    private final LabTestRepository labTestRepository;
    private final PanelRepository   panelRepository;
    private final AuditService      auditService;

    // ── LabTest CRUD ─────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_LAB_MANAGER')")
    public LabTest createTest(LabTest test) {
        if (labTestRepository.existsByCodeAndIsDeletedFalse(test.getCode()))
            throw new DuplicateResourceException("LabTest", "code", test.getCode());
        LabTest saved = labTestRepository.save(test);
        auditService.log("LAB_TEST_CREATED", "LabTest", saved.getId(), "code=" + test.getCode());
        return saved;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public LabTest findTestById(Long id) {
        return labTestRepository.findById(id)
            .filter(t -> !t.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("LabTest", id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<LabTest> findAllTests(Pageable pageable) {
        return labTestRepository.findAllActive(pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<LabTest> searchTests(String query, Pageable pageable) {
        return labTestRepository.search(query, pageable);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_LAB_MANAGER')")
    public LabTest updateTest(Long id, LabTest updates) {
        LabTest existing = findTestById(id);
        existing.setName(updates.getName());
        existing.setDescription(updates.getDescription());
        existing.setSpecimenType(updates.getSpecimenType());
        existing.setTurnaroundHours(updates.getTurnaroundHours());
        existing.setPrice(updates.getPrice());
        existing.setReferenceRange(updates.getReferenceRange());
        existing.setActive(updates.isActive());
        LabTest saved = labTestRepository.save(existing);
        auditService.log("LAB_TEST_UPDATED", "LabTest", id, "code=" + existing.getCode());
        return saved;
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void deleteTest(Long id) {
        LabTest test = findTestById(id);
        test.setDeleted(true);
        test.setActive(false);
        labTestRepository.save(test);
        auditService.log("LAB_TEST_DELETED", "LabTest", id, "code=" + test.getCode());
    }

    // ── Panel CRUD ────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_LAB_MANAGER')")
    public Panel createPanel(Panel panel, Set<Long> testIds) {
        if (panelRepository.existsByCodeAndIsDeletedFalse(panel.getCode()))
            throw new DuplicateResourceException("Panel", "code", panel.getCode());

        Set<LabTest> tests = testIds.stream()
            .map(this::findTestById)
            .collect(Collectors.toSet());
        panel.setTests(tests);

        Panel saved = panelRepository.save(panel);
        auditService.log("PANEL_CREATED", "Panel", saved.getId(), "code=" + panel.getCode());
        return saved;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Panel findPanelById(Long id) {
        return panelRepository.findById(id)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Panel", id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Panel> findAllPanels(Pageable pageable) {
        return panelRepository.findAllActive(pageable);
    }
}
