package com.cognizant.medlab.web.thymeleaf;

import com.cognizant.medlab.application.service.*;
import com.cognizant.medlab.domain.identity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Thymeleaf MVC controller — serves server-rendered HTML pages.
 *
 * All list pages default to page 0, size 20, and pass data to templates.
 * Dynamic interactions (search, status updates) use plain JS fetch() to
 * the REST API (/api/**) — no full page reload needed.
 */
@Controller
@RequiredArgsConstructor
public class UiController {

    private final PatientService     patientService;
    private final AppointmentService appointmentService;
    private final SampleService      sampleService;
    private final TestOrderService   testOrderService;
    private final ReportService      reportService;
    private final BillingService     billingService;
    private final NotificationService notificationService;
    private final AuthService        authService;

    // ── Public pages ──────────────────────────────────────────────

    @GetMapping("/")
    public String root() { return "redirect:/ui/dashboard"; }

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    // ── Dashboard ─────────────────────────────────────────────────

    @GetMapping("/ui/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("user", user);
        model.addAttribute("patientCount",
            patientService.findAll(PageRequest.of(0, 1)).getTotalElements());
        model.addAttribute("pendingOrders",
            testOrderService.findAll(PageRequest.of(0, 1)).getTotalElements());
        model.addAttribute("recentReports",
            reportService.findAll(PageRequest.of(0, 5,
                Sort.by(Sort.Direction.DESC, "generatedAt"))).getContent());
        return "dashboard";
    }

    // ── Patients ──────────────────────────────────────────────────

    @GetMapping("/ui/patients")
    public String patients(Model model,
                           @RequestParam(defaultValue = "0")  int page,
                           @RequestParam(defaultValue = "20") int size,
                           @RequestParam(required = false)    String search) {
        var pageable = PageRequest.of(page, size, Sort.by("fullName"));
        var result = (search != null && !search.isBlank())
            ? patientService.search(search, pageable)
            : patientService.findAll(pageable);
        model.addAttribute("patients", result);
        model.addAttribute("search", search);
        return "patients/list";
    }

    @GetMapping("/ui/patients/{id}")
    public String patientDetail(@PathVariable Long id, Model model) {
        model.addAttribute("patient", patientService.findById(id));
        model.addAttribute("appointments",
            appointmentService.findByPatient(id, PageRequest.of(0, 10)).getContent());
        model.addAttribute("reports",
            reportService.findByPatient(id, PageRequest.of(0, 10)).getContent());
        return "patients/detail";
    }

    @GetMapping("/ui/patients/new")
    public String newPatientForm(Model model) {
        model.addAttribute("genders",
            com.cognizant.medlab.domain.patient.Patient.Gender.values());
        return "patients/form";
    }

    // ── Appointments ──────────────────────────────────────────────

    @GetMapping("/ui/appointments")
    public String appointments(Model model,
                               @RequestParam(defaultValue = "0")  int page,
                               @RequestParam(defaultValue = "20") int size) {
        model.addAttribute("appointments",
            appointmentService.findAll(PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "scheduledAt"))));
        return "appointments/list";
    }

    @GetMapping("/ui/appointments/new")
    public String newAppointmentForm(Model model) {
        model.addAttribute("patients",
            patientService.findAll(PageRequest.of(0, 100)).getContent());
        return "appointments/form";
    }

    // ── Samples ───────────────────────────────────────────────────

    @GetMapping("/ui/samples")
    public String samples(Model model,
                          @RequestParam(defaultValue = "0")  int page,
                          @RequestParam(defaultValue = "20") int size) {
        model.addAttribute("samples",
            sampleService.findAll(PageRequest.of(page, size)));
        model.addAttribute("specimenTypes",
            com.cognizant.medlab.domain.scheduling.Sample.SpecimenType.values());
        return "samples/list";
    }

    // ── Test Orders ───────────────────────────────────────────────

    @GetMapping("/ui/testorders")
    public String testOrders(Model model,
                             @RequestParam(defaultValue = "0")  int page,
                             @RequestParam(defaultValue = "20") int size) {
        model.addAttribute("orders",
            testOrderService.findAll(PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"))));
        return "testorders/list";
    }

    // ── Reports ───────────────────────────────────────────────────

    @GetMapping("/ui/reports")
    public String reports(Model model,
                          @RequestParam(defaultValue = "0")  int page,
                          @RequestParam(defaultValue = "20") int size) {
        model.addAttribute("reports",
            reportService.findAll(PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "generatedAt"))));
        return "reports/list";
    }

    @GetMapping("/ui/reports/{id}")
    public String reportDetail(@PathVariable Long id, Model model) {
        model.addAttribute("report", reportService.findById(id));
        return "reports/detail";
    }

    @GetMapping("/ui/reports/{id}/download")
    public org.springframework.http.ResponseEntity<byte[]> downloadReport(@PathVariable Long id) {
        com.cognizant.medlab.domain.reporting.Report report = reportService.findById(id);
        byte[] content = (report.getSummary() != null
            ? report.getSummary()
            : "No summary available.").getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return org.springframework.http.ResponseEntity.ok()
            .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + report.getReportNumber() + ".txt\"")
            .body(content);
    }

    // ── Billing ───────────────────────────────────────────────────

    @GetMapping("/ui/billing")
    public String billing(Model model,
                          @RequestParam(defaultValue = "0")  int page,
                          @RequestParam(defaultValue = "20") int size) {
        model.addAttribute("invoices",
            billingService.findAllInvoices(PageRequest.of(page, size)));
        return "billing/list";
    }

    // ── Notifications ─────────────────────────────────────────────

    @GetMapping("/ui/notifications")
    public String notifications(Model model,
                                @RequestParam(defaultValue = "0")  int page,
                                @RequestParam(defaultValue = "20") int size) {
        model.addAttribute("notifications",
            notificationService.getAll(PageRequest.of(page, size)));
        return "notifications/list";
    }

    // ── Admin ─────────────────────────────────────────────────────

    @GetMapping("/ui/admin/users")
    public String adminUsers(Model model,
                             @RequestParam(defaultValue = "0")  int page,
                             @RequestParam(defaultValue = "20") int size) {
        model.addAttribute("users",
            authService.getAllUsers(PageRequest.of(page, size)));
        return "admin/users";
    }

    @GetMapping("/ui/admin/auditlogs")
    public String auditLogs(Model model, @AuthenticationPrincipal User user) {
        // Only summary — full paged list uses JS/API call
        model.addAttribute("user", user);
        return "admin/auditlogs";
    }
}
