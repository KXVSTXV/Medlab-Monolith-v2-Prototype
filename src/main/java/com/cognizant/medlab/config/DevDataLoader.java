package com.cognizant.medlab.config;

import com.cognizant.medlab.domain.identity.Role;
import com.cognizant.medlab.domain.identity.User;
import com.cognizant.medlab.domain.patient.Patient;
import com.cognizant.medlab.domain.scheduling.*;
import com.cognizant.medlab.domain.processing.*;
import com.cognizant.medlab.domain.reporting.Report;
import com.cognizant.medlab.domain.testcatalog.LabTest;
import com.cognizant.medlab.domain.testcatalog.Panel;
import com.cognizant.medlab.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Dev data loader — seeds ALL reference data (roles, users, patients,
 * lab tests, panels) PLUS transactional data (appointments, samples,
 * orders, reports).
 *
 * This replaces Flyway V2 seed when running on H2 (Flyway disabled).
 * It is also safe to run with MySQL + Flyway because it checks existence
 * before inserting anything.
 *
 * Active only when:
 *   spring.profiles.active=dev  AND  app.data-loader.enabled=true
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.data-loader.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DevDataLoader implements ApplicationRunner {

    private final RoleRepository        roleRepository;
    private final UserRepository        userRepository;
    private final PatientRepository     patientRepository;
    private final LabTestRepository     labTestRepository;
    private final PanelRepository       panelRepository;
    private final AppointmentRepository appointmentRepository;
    private final SampleRepository      sampleRepository;
    private final TestOrderRepository   testOrderRepository;
    private final TestResultRepository  testResultRepository;
    private final ReportRepository      reportRepository;
    private final PasswordEncoder       passwordEncoder;

    private static final String PASSWORD = "Admin@123";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("[DevDataLoader] Starting...");

        seedRoles();
        seedUsers();
        seedPatients();
        seedLabTests();
        seedPanels();
        seedTransactionalData();

        log.info("[DevDataLoader] Complete.");
    }

    // ── Roles ─────────────────────────────────────────────────────

    private void seedRoles() {
        List<String> names = List.of(
            Role.ADMIN, Role.LAB_MANAGER, Role.LAB_TECH,
            Role.DOCTOR, Role.RECEPTION, Role.BILLING);
        for (String name : names) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(new Role(null, name));
                log.info("[DevDataLoader] Role created: {}", name);
            }
        }
    }

    // ── Users ─────────────────────────────────────────────────────

    private void seedUsers() {
        createUser("admin",       "admin@medlab.com",       "System Administrator", Role.ADMIN);
        createUser("labmanager1", "labmanager@medlab.com",  "Lab Manager One",      Role.LAB_MANAGER);
        createUser("labtech1",    "labtech1@medlab.com",    "Lab Technician One",   Role.LAB_TECH);
        createUser("labtech2",    "labtech2@medlab.com",    "Lab Technician Two",   Role.LAB_TECH);
        createUser("doctor1",     "doctor1@medlab.com",     "Dr. Priya Sharma",     Role.DOCTOR);
        createUser("reception1",  "reception1@medlab.com",  "Receptionist One",     Role.RECEPTION);
        createUser("billing1",    "billing1@medlab.com",    "Billing Staff One",    Role.BILLING);
    }

    private void createUser(String username, String email, String fullName, String roleName) {
        if (userRepository.findByUsernameAndIsDeletedFalse(username).isPresent()) return;
        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));
        User user = User.builder()
            .username(username)
            .email(email)
            .passwordHash(passwordEncoder.encode(PASSWORD))
            .fullName(fullName)
            .status(User.UserStatus.ACTIVE)
            .roles(Set.of(role))
            .build();
        userRepository.save(user);
        log.info("[DevDataLoader] User created: {} ({})", username, roleName);
    }

    // ── Patients ──────────────────────────────────────────────────

    private void seedPatients() {
        if (patientRepository.count() > 0) return;

        Object[][] data = {
            {"John Doe",      "2000-05-15", Patient.Gender.M, "johndoe@example.com",   "9876543210", "O+",  true},
            {"Jane Smith",    "1995-08-22", Patient.Gender.F, "janesmith@example.com", "9876543211", "A+",  true},
            {"Richard Roe",   "1988-03-10", Patient.Gender.M, "richardroe@example.com","9876543212", "B+",  true},
            {"Emily Johnson", "2002-11-30", Patient.Gender.F, "emily@example.com",     "9876543213", "AB-", true},
            {"Michael Brown", "1975-07-04", Patient.Gender.M, "michael@example.com",   "9876543214", "O-",  true},
            {"Sarah Wilson",  "1990-12-18", Patient.Gender.F, "sarah@example.com",     "9876543215", "A-",  true},
            {"David Lee",     "1982-04-25", Patient.Gender.M, "davidlee@example.com",  "9876543216", "B-",  false},
            {"Priya Patel",   "1998-09-14", Patient.Gender.F, "priya@example.com",     "9876543217", "O+",  true},
            {"Ahmed Khan",    "1965-01-07", Patient.Gender.M, "ahmed@example.com",     "9876543218", "A+",  true},
            {"Meera Nair",    "1993-06-19", Patient.Gender.F, "meera@example.com",     "9876543219", "AB+", true},
        };

        int seq = 1;
        for (Object[] row : data) {
            Patient p = Patient.builder()
                .medicalRecordNumber(String.format("MRN-%06d", seq++))
                .fullName((String) row[0])
                .dob(LocalDate.parse((String) row[1]))
                .gender((Patient.Gender) row[2])
                .email((String) row[3])
                .mobileNumber((String) row[4])
                .bloodGroup((String) row[5])
                .consentGiven((Boolean) row[6])
                .status(Patient.PatientStatus.ACTIVE)
                .build();
            patientRepository.save(p);
        }
        log.info("[DevDataLoader] {} patients seeded.", data.length);
    }

    // ── Lab Tests ─────────────────────────────────────────────────

    private void seedLabTests() {
        if (labTestRepository.count() > 0) return;

        Object[][] tests = {
            {"CBC",         "Complete Blood Count",                "BLOOD",  4,  250.00, null},
            {"CMP",         "Comprehensive Metabolic Panel",       "BLOOD",  6,  450.00, null},
            {"LIPID",       "Lipid Profile",                       "BLOOD",  8,  350.00, null},
            {"COVID-PCR",   "COVID-19 RT-PCR",                     "SWAB",  24, 1200.00, null},
            {"URINE-RE",    "Urine Routine & Microscopy",          "URINE",  2,  150.00, null},
            {"THYROID",     "Thyroid Function Test (TSH/T3/T4)",   "BLOOD", 12,  550.00, null},
            {"BLOOD-SUGAR", "Fasting Blood Sugar",                 "BLOOD",  2,   80.00, "70-100 mg/dL"},
            {"HBA1C",       "Glycated Haemoglobin (HbA1c)",        "BLOOD",  6,  300.00, "< 5.7 %"},
            {"DENGUE-NS1",  "Dengue NS1 Antigen",                  "BLOOD",  4,  400.00, "Negative"},
            {"MALARIA",     "Malaria Antigen Test",                "BLOOD",  2,  200.00, "Negative"},
        };

        for (Object[] row : tests) {
            LabTest t = LabTest.builder()
                .code((String) row[0])
                .name((String) row[1])
                .specimenType((String) row[2])
                .turnaroundHours((Integer) row[3])
                .price(BigDecimal.valueOf((Double) row[4]))
                .referenceRange((String) row[5])
                .active(true)
                .build();
            labTestRepository.save(t);
        }
        log.info("[DevDataLoader] {} lab tests seeded.", tests.length);
    }

    // ── Panels ────────────────────────────────────────────────────

    private void seedPanels() {
        if (panelRepository.count() > 0) return;

        LabTest cmp   = labTestRepository.findByCodeAndIsDeletedFalse("CMP").orElse(null);
        LabTest sugar = labTestRepository.findByCodeAndIsDeletedFalse("BLOOD-SUGAR").orElse(null);
        LabTest hba1c = labTestRepository.findByCodeAndIsDeletedFalse("HBA1C").orElse(null);

        if (cmp != null && sugar != null) {
            Panel bmp = Panel.builder()
                .code("BMP").name("Basic Metabolic Panel")
                .description("Core metabolic markers")
                .tests(Set.of(cmp, sugar)).active(true).build();
            panelRepository.save(bmp);
        }
        if (sugar != null && hba1c != null) {
            Panel diab = Panel.builder()
                .code("DIAB").name("Diabetes Screening")
                .description("Blood sugar + HbA1c bundle")
                .tests(Set.of(sugar, hba1c)).active(true).build();
            panelRepository.save(diab);
        }
        log.info("[DevDataLoader] Panels seeded.");
    }

    // ── Transactional data ────────────────────────────────────────

    private void seedTransactionalData() {
        if (appointmentRepository.count() > 0) {
            log.info("[DevDataLoader] Transactional data already exists — skipping.");
            return;
        }

        List<Patient> patients = patientRepository.findAll().stream()
            .filter(p -> !p.isDeleted() && p.isConsentGiven()).limit(3).toList();
        List<LabTest> tests = labTestRepository.findAll().stream()
            .filter(t -> !t.isDeleted() && t.isActive()).limit(2).toList();

        if (patients.isEmpty() || tests.isEmpty()) {
            log.warn("[DevDataLoader] Not enough data to seed appointments.");
            return;
        }

        for (int i = 0; i < patients.size(); i++) {
            Patient patient = patients.get(i);

            Appointment appt = appointmentRepository.save(Appointment.builder()
                .patient(patient)
                .scheduledAt(LocalDateTime.now().minusDays(i + 1))
                .location("Lab Branch " + (i % 2 + 1))
                .status(Appointment.AppointmentStatus.SAMPLE_COLLECTED)
                .build());

            Sample sample = sampleRepository.save(Sample.builder()
                .appointment(appt)
                .barcode("BL-DEV-" + String.format("%04d", i + 1))
                .specimenType(Sample.SpecimenType.BLOOD)
                .collectedAt(LocalDateTime.now().minusDays(i + 1).plusHours(1))
                .collectedBy("labtech1")
                .status(Sample.SampleStatus.ANALYSED)
                .build());

            for (LabTest labTest : tests) {
                boolean abnormal = (i == 1);
                TestOrder order = testOrderRepository.save(TestOrder.builder()
                    .sample(sample).labTest(labTest).orderedBy("doctor1")
                    .priority(TestOrder.Priority.ROUTINE)
                    .status(TestOrder.TestOrderStatus.VERIFIED)
                    .build());

                testResultRepository.save(TestResult.builder()
                    .testOrder(order)
                    .value(abnormal ? "350" : "85")
                    .unit("mg/dL")
                    .referenceRange(labTest.getReferenceRange())
                    .abnormal(abnormal)
                    .verifiedBy("labtech1")
                    .verifiedAt(LocalDateTime.now().minusDays(i))
                    .status(TestResult.ResultStatus.VERIFIED)
                    .build());
            }

            if (i < 2) {
                reportRepository.save(Report.builder()
                    .reportNumber("RPT-DEV-" + String.format("%04d", i + 1))
                    .patient(patient).appointment(appt)
                    .generatedAt(LocalDateTime.now().minusDays(i))
                    .hasAbnormal(i == 1)
                    .preparedBy("labtech1").verifiedBy("labmanager1")
                    .status(i == 0 ? Report.ReportStatus.RELEASED : Report.ReportStatus.VERIFIED)
                    .summary("Dev seed report for " + patient.getMedicalRecordNumber())
                    .pdfRef("reports/RPT-DEV-" + String.format("%04d", i + 1) + ".txt")
                    .build());
            }
        }

        log.info("[DevDataLoader] Transactional data seeded.");
    }
}
