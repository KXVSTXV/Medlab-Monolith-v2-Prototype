package com.cognizant.medlab.application.service;

import com.cognizant.medlab.domain.notification.NotificationLog.NotificationType;
import com.cognizant.medlab.domain.processing.TestResult;
import com.cognizant.medlab.domain.reporting.Report;
import com.cognizant.medlab.domain.scheduling.Appointment;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.lang.NonNull;
/**
 * Report Service — mandatory core service (Project 10).
 *
 * Generates consolidated lab reports aggregating all TestResults
 * for a given Appointment. PDF reference is stored as a path string
 * (plaintext report in v2; swap iText/OpenPDF dependency for real PDF in v3).
 *
 * Lifecycle: DRAFT → VERIFIED → RELEASED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository     reportRepository;
    private final AppointmentRepository appointmentRepository;
    private final TestResultRepository testResultRepository;
    private final NotificationService  notificationService;
    private final AuditService         auditService;

    private static final DateTimeFormatter RPT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── Generate (creates DRAFT) ──────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_LAB_TECH','ROLE_LAB_MANAGER')")
    public Report generateReport(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .filter(a -> !a.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        // Prevent duplicate reports
        if (reportRepository.findByAppointmentIdAndIsDeletedFalse(appointmentId).isPresent())
            throw new BusinessRuleException(
                "A report already exists for appointment #" + appointmentId
                + ". Use verify/release endpoints to advance it.");

        List<TestResult> results = testResultRepository.findByAppointmentId(appointmentId);
        if (results.isEmpty())
            throw new BusinessRuleException(
                "No test results found for appointment #" + appointmentId
                + ". Enter results before generating a report.");

        boolean hasAbnormal = results.stream().anyMatch(TestResult::isAbnormal);
        String preparedBy   = SecurityContextHolder.getContext().getAuthentication().getName();
        String reportNumber = generateReportNumber();
        String summary      = buildSummary(results);

        Report report = Report.builder()
            .reportNumber(reportNumber)
            .patient(appointment.getPatient())
            .appointment(appointment)
            .generatedAt(LocalDateTime.now())
            .hasAbnormal(hasAbnormal)
            .preparedBy(preparedBy)
            .status(Report.ReportStatus.DRAFT)
            .summary(summary)
            .pdfRef("reports/" + reportNumber + ".txt")   // v3: replace with S3 key
            .build();

        Report saved = reportRepository.save(report);

        auditService.log("REPORT_GENERATED", "Report", saved.getId(),
                         "reportNumber=" + reportNumber + ", abnormal=" + hasAbnormal);
        log.info("Report {} generated for appointment #{}", reportNumber, appointmentId);
        return saved;
    }

    // ── Verify ────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_LAB_MANAGER','ROLE_DOCTOR')")
    public Report verifyReport(Long reportId) {
        Report report = findById(reportId);

        if (report.getStatus() != Report.ReportStatus.DRAFT)
            throw new BusinessRuleException(
                "Only DRAFT reports can be verified. Current: " + report.getStatus());

        String verifier = SecurityContextHolder.getContext().getAuthentication().getName();
        report.setVerifiedBy(verifier);
        report.setStatus(Report.ReportStatus.VERIFIED);
        Report saved = reportRepository.save(report);

        auditService.log("REPORT_VERIFIED", "Report", reportId, "verifiedBy=" + verifier);
        return saved;
    }

    // ── Release ───────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_LAB_MANAGER')")
    public Report releaseReport(Long reportId) {
        Report report = findById(reportId);

        if (report.getStatus() != Report.ReportStatus.VERIFIED)
            throw new BusinessRuleException(
                "Only VERIFIED reports can be released. Current: " + report.getStatus());

        report.setStatus(Report.ReportStatus.RELEASED);
        Report saved = reportRepository.save(report);

        // Notify patient
        String msg = "Your lab report " + report.getReportNumber() + " is ready."
            + (report.isHasAbnormal()
               ? " ⚠ Abnormal values detected — please consult your doctor." : "");
        notificationService.send(report.getPatient().getId(), msg, NotificationType.REPORT_READY);

        // Advance appointment to COMPLETED
        Appointment appointment = report.getAppointment();
        if (appointment != null) {
            appointment.setStatus(Appointment.AppointmentStatus.COMPLETED);
            appointmentRepository.save(appointment);
        }

        auditService.log("REPORT_RELEASED", "Report", reportId,
                         "reportNumber=" + report.getReportNumber());
        log.info("Report {} released", report.getReportNumber());
        return saved;
    }

    // ── Query ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Report findById(@NonNull Long id) {
        return reportRepository.findById(id)
            .filter(r -> !r.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Report", id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Report> findAll(Pageable pageable) {
        return reportRepository.findAllActive(pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Report> findByPatient(Long patientId, Pageable pageable) {
        return reportRepository.findByPatientId(patientId, pageable);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String generateReportNumber() {
        String year  = String.valueOf(LocalDateTime.now().getYear());
        String uid   = String.format("%06d", (int)(Math.random() * 1_000_000));
        return "RPT-" + year + "-" + uid;
    }

    private String buildSummary(List<TestResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generated: ").append(LocalDateTime.now().format(RPT_FMT)).append("\n\n");
        sb.append(String.format("%-30s %-15s %-15s %-10s%n",
                                "Test", "Result", "Reference", "Status"));
        sb.append("-".repeat(75)).append("\n");
        for (TestResult r : results) {
            String testName = r.getTestOrder().getLabTest().getName();
            sb.append(String.format("%-30s %-15s %-15s %-10s%n",
                truncate(testName, 29),
                r.getValue() + (r.getUnit() != null ? " " + r.getUnit() : ""),
                r.getReferenceRange() != null ? r.getReferenceRange() : "N/A",
                r.isAbnormal() ? "⚠ ABNORMAL" : "NORMAL"));
        }
        return sb.toString();
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
