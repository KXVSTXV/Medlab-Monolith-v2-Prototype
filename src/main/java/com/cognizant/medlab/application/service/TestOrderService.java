package com.cognizant.medlab.application.service;

import com.cognizant.medlab.domain.notification.NotificationLog.NotificationType;
import com.cognizant.medlab.domain.processing.*;
import com.cognizant.medlab.domain.scheduling.Sample;
import com.cognizant.medlab.domain.testcatalog.LabTest;
import com.cognizant.medlab.exception.*;
import com.cognizant.medlab.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Test Order Service — mandatory core service (Project 10).
 *
 * Manages the full lifecycle of a lab test order from creation through
 * result entry and verification.
 *
 * Status lifecycle:
 *   ORDERED → SAMPLE_COLLECTED → IN_PROGRESS → RESULT_ENTERED → VERIFIED → COMPLETED
 *                                                                          → CANCELLED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestOrderService {

    private final TestOrderRepository  testOrderRepository;
    private final TestResultRepository testResultRepository;
    private final SampleRepository     sampleRepository;
    private final LabTestRepository    labTestRepository;
    private final NotificationService  notificationService;
    private final AuditService         auditService;

    // ── Create test order ─────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPTION','ROLE_DOCTOR','ROLE_LAB_MANAGER')")
    public TestOrder createOrder(Long sampleId, Long labTestId,
                                 TestOrder.Priority priority, String notes) {
        Sample sample = sampleRepository.findById(sampleId)
            .filter(s -> !s.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Sample", sampleId));

        LabTest labTest = labTestRepository.findById(labTestId)
            .filter(t -> !t.isDeleted() && t.isActive())
            .orElseThrow(() -> new ResourceNotFoundException("LabTest", labTestId));

        String orderedBy = SecurityContextHolder.getContext().getAuthentication().getName();

        TestOrder order = TestOrder.builder()
            .sample(sample)
            .labTest(labTest)
            .orderedBy(orderedBy)
            .priority(priority)
            .notes(notes)
            .status(TestOrder.TestOrderStatus.ORDERED)
            .build();

        TestOrder saved = testOrderRepository.save(order);

        notificationService.send(
            sample.getAppointment().getPatient().getId(),
            "Test order #" + saved.getId() + " for '" + labTest.getName()
                + "' has been placed. Priority: " + priority,
            NotificationType.ORDER_CREATED
        );

        auditService.log("TEST_ORDER_CREATED", "TestOrder", saved.getId(),
                         "test=" + labTest.getCode() + ", priority=" + priority);
        log.info("TestOrder #{} created for test '{}'", saved.getId(), labTest.getCode());
        return saved;
    }

    // ── Submit result ─────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_LAB_TECH','ROLE_LAB_MANAGER')")
    public TestResult submitResult(Long testOrderId, String value, String unit,
                                   String referenceRange, String remarks) {
        TestOrder order = findById(testOrderId);

        if (order.getStatus() != TestOrder.TestOrderStatus.IN_PROGRESS
                && order.getStatus() != TestOrder.TestOrderStatus.SAMPLE_COLLECTED
                && order.getStatus() != TestOrder.TestOrderStatus.ORDERED)
            throw new BusinessRuleException(
                "Results can only be entered for orders in ORDERED/SAMPLE_COLLECTED/IN_PROGRESS state.");

        boolean abnormal = detectAbnormal(value, referenceRange);

        TestResult result = TestResult.builder()
            .testOrder(order)
            .value(value)
            .unit(unit)
            .referenceRange(referenceRange)
            .remarks(remarks)
            .abnormal(abnormal)
            .status(TestResult.ResultStatus.ENTERED)
            .build();

        TestResult saved = testResultRepository.save(result);

        order.setStatus(TestOrder.TestOrderStatus.RESULT_ENTERED);
        testOrderRepository.save(order);

        if (abnormal) log.warn("ABNORMAL result for TestOrder #{}: {}", testOrderId, value);
        auditService.log("RESULT_ENTERED", "TestResult", saved.getId(),
                         "orderId=" + testOrderId + ", abnormal=" + abnormal);
        return saved;
    }

    // ── Verify result ─────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_LAB_TECH','ROLE_DOCTOR','ROLE_LAB_MANAGER')")
    public TestResult verifyResult(Long testOrderId) {
        TestOrder order = findById(testOrderId);

        if (order.getStatus() != TestOrder.TestOrderStatus.RESULT_ENTERED)
            throw new BusinessRuleException(
                "Only RESULT_ENTERED orders can be verified. Current: " + order.getStatus());

        TestResult result = testResultRepository.findByTestOrderIdAndIsDeletedFalse(testOrderId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "TestResult for order " + testOrderId));

        String verifier = SecurityContextHolder.getContext().getAuthentication().getName();
        result.setVerifiedBy(verifier);
        result.setVerifiedAt(LocalDateTime.now());
        result.setStatus(TestResult.ResultStatus.VERIFIED);
        testResultRepository.save(result);

        order.setStatus(TestOrder.TestOrderStatus.VERIFIED);
        testOrderRepository.save(order);

        auditService.log("RESULT_VERIFIED", "TestResult", result.getId(),
                         "verifiedBy=" + verifier);
        return result;
    }

    // ── Cancel order ──────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_RECEPTION','ROLE_LAB_MANAGER')")
    public void cancelOrder(Long testOrderId) {
        TestOrder order = findById(testOrderId);

        if (order.getStatus() == TestOrder.TestOrderStatus.COMPLETED)
            throw new BusinessRuleException("Cannot cancel a COMPLETED order.");

        order.setStatus(TestOrder.TestOrderStatus.CANCELLED);
        order.setDeleted(true);
        testOrderRepository.save(order);

        Long patientId = order.getSample().getAppointment().getPatient().getId();
        notificationService.send(patientId,
            "Your test order #" + testOrderId + " for '"
                + order.getLabTest().getName() + "' has been cancelled.",
            NotificationType.ORDER_CANCELLED);

        auditService.log("TEST_ORDER_CANCELLED", "TestOrder", testOrderId, "");
    }

    // ── Query ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public TestOrder findById(Long id) {
        return testOrderRepository.findById(id)
            .filter(o -> !o.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("TestOrder", id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<TestOrder> findAll(Pageable pageable) {
        return testOrderRepository.findAllActive(pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<TestOrder> findBySample(Long sampleId) {
        return testOrderRepository.findBySampleId(sampleId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<TestOrder> findByPatient(Long patientId, Pageable pageable) {
        return testOrderRepository.findByPatientId(patientId, pageable);
    }

    // ── Abnormal detection (ported from CLI v1 ReportService) ─────

    public static boolean detectAbnormal(String value, String referenceRange) {
        if (referenceRange == null || referenceRange.isBlank()) return false;
        try {
            double v = Double.parseDouble(value.trim().replaceAll("[^0-9.]", ""));
            String range = referenceRange.trim();
            if (range.startsWith("<")) {
                return v >= Double.parseDouble(range.substring(1).trim());
            } else if (range.startsWith(">")) {
                return v <= Double.parseDouble(range.substring(1).trim());
            } else if (range.contains("-")) {
                String[] p = range.split("-");
                double min = Double.parseDouble(p[0].trim());
                double max = Double.parseDouble(p[1].trim());
                return v < min || v > max;
            }
        } catch (NumberFormatException ignored) {
            // Non-numeric results (Negative/Positive) — not auto-flagged
        }
        return false;
    }
}
