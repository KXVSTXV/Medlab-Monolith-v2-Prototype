package com.cognizant.medlab.application.service;

import com.cognizant.medlab.domain.patient.Patient;
import com.cognizant.medlab.exception.BusinessRuleException;
import com.cognizant.medlab.exception.DuplicateResourceException;
import com.cognizant.medlab.exception.ResourceNotFoundException;
import com.cognizant.medlab.repository.PatientRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PatientService.
 * Uses Mockito — no Spring context loaded (fast).
 * Test coverage target: ≥ 80% as per JaCoCo config.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PatientService Unit Tests")
class PatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private AuditService      auditService;

    @InjectMocks private PatientService patientService;

    private Patient samplePatient;

    @BeforeEach
    void setUp() {
        samplePatient = Patient.builder()
            .id(1L)
            .medicalRecordNumber("MRN-000001")
            .fullName("John Doe")
            .dob(LocalDate.of(2000, 5, 15))
            .gender(Patient.Gender.M)
            .email("john@example.com")
            .mobileNumber("9876543210")
            .consentGiven(true)
            .status(Patient.PatientStatus.ACTIVE)
            .build();
    }

    // ── register ──────────────────────────────────────────────────

    @Test
    @DisplayName("register: saves patient and assigns MRN")
    void register_success_assignsMrn() {
        when(patientRepository.existsByEmailAndIsDeletedFalse("john@example.com"))
            .thenReturn(false);
        when(patientRepository.existsByMobileNumberAndIsDeletedFalse("9876543210"))
            .thenReturn(false);
        // First save returns patient with id=42
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> {
            Patient p = inv.getArgument(0);
            p.setId(42L);
            return p;
        });

        Patient result = patientService.register(samplePatient);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getMedicalRecordNumber()).isEqualTo("MRN-000042");
        verify(patientRepository, times(2)).save(any()); // save + MRN update
        verify(auditService).log(eq("PATIENT_REGISTERED"), eq("Patient"), eq(42L), anyString());
    }

    @Test
    @DisplayName("register: throws DuplicateResourceException when email already exists")
    void register_duplicateEmail_throws() {
        when(patientRepository.existsByEmailAndIsDeletedFalse("john@example.com"))
            .thenReturn(true);

        assertThatThrownBy(() -> patientService.register(samplePatient))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("email");

        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: throws DuplicateResourceException when mobile already exists")
    void register_duplicateMobile_throws() {
        when(patientRepository.existsByEmailAndIsDeletedFalse(any())).thenReturn(false);
        when(patientRepository.existsByMobileNumberAndIsDeletedFalse("9876543210"))
            .thenReturn(true);

        assertThatThrownBy(() -> patientService.register(samplePatient))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("mobileNumber");
    }

    // ── findById ──────────────────────────────────────────────────

    @Test
    @DisplayName("findById: returns patient when exists and not deleted")
    void findById_found() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));

        Patient result = patientService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getFullName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("findById: throws ResourceNotFoundException for missing patient")
    void findById_notFound_throws() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("findById: throws ResourceNotFoundException for soft-deleted patient")
    void findById_deleted_throws() {
        samplePatient.setDeleted(true);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));

        assertThatThrownBy(() -> patientService.findById(1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── findAll ───────────────────────────────────────────────────

    @Test
    @DisplayName("findAll: returns paged result from repository")
    void findAll_returnsPaged() {
        Page<Patient> page = new PageImpl<>(List.of(samplePatient));
        when(patientRepository.findAllActive(any(Pageable.class))).thenReturn(page);

        Page<Patient> result = patientService.findAll(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("John Doe");
    }

    // ── search ────────────────────────────────────────────────────

    @Test
    @DisplayName("search: delegates to repository search method")
    void search_delegatesToRepo() {
        Page<Patient> page = new PageImpl<>(List.of(samplePatient));
        when(patientRepository.search(eq("John"), any(Pageable.class))).thenReturn(page);

        Page<Patient> result = patientService.search("John", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(patientRepository).search(eq("John"), any(Pageable.class));
    }

    // ── update ────────────────────────────────────────────────────

    @Test
    @DisplayName("update: saves updated fields and returns patient")
    void update_success() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Patient updates = Patient.builder()
            .fullName("John Updated")
            .dob(LocalDate.of(2000, 5, 15))
            .gender(Patient.Gender.M)
            .consentGiven(true)
            .build();

        Patient result = patientService.update(1L, updates);

        assertThat(result.getFullName()).isEqualTo("John Updated");
        verify(auditService).log(eq("PATIENT_UPDATED"), eq("Patient"), eq(1L), anyString());
    }

    // ── softDelete ────────────────────────────────────────────────

    @Test
    @DisplayName("softDelete: marks patient as deleted")
    void softDelete_setsDeletedFlag() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        patientService.softDelete(1L);

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
        assertThat(captor.getValue().getStatus()).isEqualTo(Patient.PatientStatus.INACTIVE);
    }

    @Test
    @DisplayName("softDelete: throws when patient not found")
    void softDelete_notFound_throws() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.softDelete(99L))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(patientRepository, never()).save(any());
    }
}
