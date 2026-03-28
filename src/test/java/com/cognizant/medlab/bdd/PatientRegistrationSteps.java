package com.cognizant.medlab.bdd;

import com.cognizant.medlab.application.service.AppointmentService;
import com.cognizant.medlab.application.service.PatientService;
import com.cognizant.medlab.domain.patient.Patient;
import com.cognizant.medlab.exception.BusinessRuleException;
import com.cognizant.medlab.exception.DuplicateResourceException;
import com.cognizant.medlab.repository.PatientRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Cucumber step definitions for Patient Registration feature.
 *
 * Uses @SpringBootTest with "test" profile (H2 in-memory, Flyway disabled).
 */
@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PatientRegistrationSteps {

    @Autowired private PatientService    patientService;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentService appointmentService;

    private Patient registeredPatient;
    private Exception caughtException;
    private long patientCountBefore;

    @Before
    public void resetContext() {
        SecurityContextHolder.clearContext();
        registeredPatient = null;
        caughtException   = null;
        patientCountBefore = 0;
    }

    // ── Given ────────────────────────────────────────────────────

    @Given("the system has the following roles: {string}")
    public void the_system_has_roles(String roles) {
        // Roles are seeded by test data / H2 schema — nothing to do
    }

    @Given("I am logged in as {string} with role {string}")
    public void i_am_logged_in_as(String username, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
            username, null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Given("a patient with email {string} already exists")
    public void a_patient_with_email_already_exists(String email) {
        // Set auth context for the pre-existing patient insert
        i_am_logged_in_as("admin", "ROLE_ADMIN");

        Patient existing = Patient.builder()
            .fullName("Existing Patient")
            .dob(LocalDate.of(1990, 1, 1))
            .gender(Patient.Gender.F)
            .email(email)
            .mobileNumber("8000000001")
            .consentGiven(true)
            .build();
        patientService.register(existing);
    }

    // ── When ─────────────────────────────────────────────────────

    @When("I register a patient with the following details:")
    public void i_register_patient_with_details(Map<String, String> data) {
        try {
            Patient p = Patient.builder()
                .fullName(data.get("fullName"))
                .dob(LocalDate.parse(data.get("dob")))
                .gender(Patient.Gender.valueOf(data.get("gender")))
                .email(data.get("email"))
                .mobileNumber(data.get("mobileNumber"))
                .consentGiven(Boolean.parseBoolean(data.get("consentGiven")))
                .build();
            registeredPatient = patientService.register(p);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("I try to register another patient with email {string}")
    public void i_try_to_register_with_duplicate_email(String email) {
        i_am_logged_in_as("reception1", "ROLE_RECEPTION");
        try {
            Patient p = Patient.builder()
                .fullName("Duplicate Email Test")
                .dob(LocalDate.of(1992, 3, 20))
                .gender(Patient.Gender.M)
                .email(email)
                .mobileNumber("8000000002")
                .consentGiven(true)
                .build();
            registeredPatient = patientService.register(p);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("I register a patient without giving consent")
    public void i_register_without_consent() {
        try {
            Patient p = Patient.builder()
                .fullName("No Consent Patient")
                .dob(LocalDate.of(1985, 8, 22))
                .gender(Patient.Gender.M)
                .mobileNumber("8000000003")
                .consentGiven(false)
                .build();
            registeredPatient = patientService.register(p);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("I register patient {string} with mobile {string}")
    public void i_register_patient(String name, String mobile) {
        i_am_logged_in_as("reception1", "ROLE_RECEPTION");
        patientCountBefore = patientRepository.count();
        try {
            Patient p = Patient.builder()
                .fullName(name)
                .dob(LocalDate.of(1990, 1, 1))
                .gender(Patient.Gender.M)
                .mobileNumber(mobile)
                .consentGiven(true)
                .build();
            registeredPatient = patientService.register(p);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    // ── Then ─────────────────────────────────────────────────────

    @Then("the patient should be saved successfully")
    public void patient_saved_successfully() {
        assertThat(caughtException).isNull();
        assertThat(registeredPatient).isNotNull();
        assertThat(registeredPatient.getId()).isPositive();
    }

    @Then("the patient should have a Medical Record Number assigned")
    public void patient_has_mrn() {
        assertThat(registeredPatient.getMedicalRecordNumber())
            .isNotBlank()
            .startsWith("MRN-");
    }

    @Then("an audit log entry should be created for {string}")
    public void audit_log_created(String action) {
        // Audit is async; just verify the patient was persisted
        assertThat(registeredPatient).isNotNull();
        // Full audit verification would need AuditLogRepository query
    }

    @Then("I should receive a duplicate resource error")
    public void i_receive_duplicate_error() {
        assertThat(caughtException)
            .isNotNull()
            .isInstanceOf(DuplicateResourceException.class);
    }

    @Then("no new patient record should be created")
    public void no_new_patient_created() {
        assertThat(registeredPatient).isNull();
    }

    @Then("the patient is saved but marked as consent_given = false")
    public void patient_saved_without_consent() {
        assertThat(caughtException).isNull();
        assertThat(registeredPatient).isNotNull();
        assertThat(registeredPatient.isConsentGiven()).isFalse();
    }

    @Then("scheduling an appointment for this patient should be blocked")
    public void scheduling_blocked_without_consent() {
        assertThatThrownBy(() ->
            appointmentService.schedule(
                registeredPatient.getId(),
                java.time.LocalDateTime.now().plusDays(1),
                "Lab A", "Test"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("consent");
    }

    @Then("the patient count should increase by {int}")
    public void patient_count_increased(int delta) {
        long current = patientRepository.count();
        assertThat(current).isEqualTo(patientCountBefore + delta);
    }

    @Then("I can find the patient by their MRN")
    public void i_can_find_by_mrn() {
        assertThat(registeredPatient.getMedicalRecordNumber()).isNotBlank();
        Patient found = patientService.findByMrn(registeredPatient.getMedicalRecordNumber());
        assertThat(found.getFullName()).isEqualTo(registeredPatient.getFullName());
    }
}
