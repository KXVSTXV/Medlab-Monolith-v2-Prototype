package com.cognizant.medlab.application.service;

import com.cognizant.medlab.domain.notification.NotificationLog;
import com.cognizant.medlab.domain.patient.Patient;
import com.cognizant.medlab.domain.processing.*;
import com.cognizant.medlab.domain.scheduling.*;
import com.cognizant.medlab.domain.testcatalog.LabTest;
import com.cognizant.medlab.exception.*;
import com.cognizant.medlab.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestOrderService (mandatory core service — Project 10).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TestOrderService Unit Tests")
class TestOrderServiceTest {

    @Mock private TestOrderRepository  testOrderRepository;
    @Mock private TestResultRepository testResultRepository;
    @Mock private SampleRepository     sampleRepository;
    @Mock private LabTestRepository    labTestRepository;
    @Mock private NotificationService  notificationService;
    @Mock private AuditService         auditService;

    @InjectMocks private TestOrderService service;

    private Sample    sample;
    private LabTest   labTest;
    private TestOrder savedOrder;
    private Patient   patient;

    @BeforeEach
    void setUp() {
        // Mock Spring Security context
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        // Build patient
        patient = Patient.builder()
            .id(1L).fullName("Jane Doe").medicalRecordNumber("MRN-000002").build();

        // Build appointment
        Appointment appointment = Appointment.builder()
            .id(10L).patient(patient)
            .scheduledAt(LocalDateTime.now())
            .status(Appointment.AppointmentStatus.SAMPLE_COLLECTED)
            .build();

        // Build sample
        sample = Sample.builder()
            .id(5L).appointment(appointment)
            .specimenType(Sample.SpecimenType.BLOOD)
            .barcode("BL-20260101-ABC123")
            .status(Sample.SampleStatus.COLLECTED)
            .build();

        // Build lab test
        labTest = LabTest.builder()
            .id(3L).code("BLOOD-SUGAR").name("Fasting Blood Sugar")
            .price(new BigDecimal("80.00"))
            .referenceRange("70-100 mg/dL")
            .active(true)
            .build();

        // Pre-built saved order
        savedOrder = TestOrder.builder()
            .id(100L).sample(sample).labTest(labTest)
            .orderedBy("testuser")
            .status(TestOrder.TestOrderStatus.ORDERED)
            .priority(TestOrder.Priority.ROUTINE)
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── createOrder ───────────────────────────────────────────────

    @Test
    @DisplayName("createOrder: saves order and sends notification")
    void createOrder_success() {
        when(sampleRepository.findById(5L)).thenReturn(Optional.of(sample));
        when(labTestRepository.findById(3L)).thenReturn(Optional.of(labTest));
        when(testOrderRepository.save(any())).thenAnswer(inv -> {
            TestOrder o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });

        TestOrder result = service.createOrder(5L, 3L, TestOrder.Priority.ROUTINE, "Fasting");

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(TestOrder.TestOrderStatus.ORDERED);
        assertThat(result.getPriority()).isEqualTo(TestOrder.Priority.ROUTINE);

        verify(testOrderRepository).save(any(TestOrder.class));
        verify(notificationService).send(
            eq(1L), contains("Fasting Blood Sugar"),
            eq(NotificationLog.NotificationType.ORDER_CREATED));
        verify(auditService).log(eq("TEST_ORDER_CREATED"), eq("TestOrder"),
                                 eq(100L), anyString());
    }

    @Test
    @DisplayName("createOrder: throws when sample not found")
    void createOrder_sampleNotFound() {
        when(sampleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(999L, 3L, TestOrder.Priority.ROUTINE, ""))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Sample");
    }

    @Test
    @DisplayName("createOrder: throws when lab test not found or inactive")
    void createOrder_labTestNotFound() {
        when(sampleRepository.findById(5L)).thenReturn(Optional.of(sample));
        when(labTestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(5L, 999L, TestOrder.Priority.URGENT, ""))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("LabTest");
    }

    // ── submitResult ──────────────────────────────────────────────

    @Test
    @DisplayName("submitResult: saves result and advances order status to RESULT_ENTERED")
    void submitResult_success_normalValue() {
        savedOrder.setStatus(TestOrder.TestOrderStatus.IN_PROGRESS);
        when(testOrderRepository.findById(100L)).thenReturn(Optional.of(savedOrder));
        when(testResultRepository.save(any())).thenAnswer(inv -> {
            TestResult r = inv.getArgument(0);
            r.setId(200L);
            return r;
        });

        TestResult result = service.submitResult(100L, "85", "mg/dL", "70-100", "Normal");

        assertThat(result.getValue()).isEqualTo("85");
        assertThat(result.isAbnormal()).isFalse();
        assertThat(result.getStatus()).isEqualTo(TestResult.ResultStatus.ENTERED);
        verify(testOrderRepository).save(argThat(o ->
            o.getStatus() == TestOrder.TestOrderStatus.RESULT_ENTERED));
    }

    @Test
    @DisplayName("submitResult: flags abnormal when value exceeds reference range")
    void submitResult_abnormal_flagged() {
        savedOrder.setStatus(TestOrder.TestOrderStatus.IN_PROGRESS);
        when(testOrderRepository.findById(100L)).thenReturn(Optional.of(savedOrder));
        when(testResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TestResult result = service.submitResult(100L, "350", "mg/dL", "70-100", "Very high");

        assertThat(result.isAbnormal()).isTrue();
    }

    @Test
    @DisplayName("submitResult: throws BusinessRuleException for COMPLETED orders")
    void submitResult_completedOrder_throws() {
        savedOrder.setStatus(TestOrder.TestOrderStatus.COMPLETED);
        when(testOrderRepository.findById(100L)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> service.submitResult(100L, "85", "mg/dL", "70-100", ""))
            .isInstanceOf(BusinessRuleException.class);
    }

    // ── verifyResult ──────────────────────────────────────────────

    @Test
    @DisplayName("verifyResult: transitions order to VERIFIED and sets verifiedBy")
    void verifyResult_success() {
        savedOrder.setStatus(TestOrder.TestOrderStatus.RESULT_ENTERED);
        TestResult entered = TestResult.builder()
            .id(200L).testOrder(savedOrder).value("85")
            .status(TestResult.ResultStatus.ENTERED).build();

        when(testOrderRepository.findById(100L)).thenReturn(Optional.of(savedOrder));
        when(testResultRepository.findByTestOrderIdAndIsDeletedFalse(100L))
            .thenReturn(Optional.of(entered));
        when(testResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TestResult result = service.verifyResult(100L);

        assertThat(result.getStatus()).isEqualTo(TestResult.ResultStatus.VERIFIED);
        assertThat(result.getVerifiedBy()).isEqualTo("testuser");
        assertThat(result.getVerifiedAt()).isNotNull();
        verify(testOrderRepository).save(argThat(o ->
            o.getStatus() == TestOrder.TestOrderStatus.VERIFIED));
    }

    @Test
    @DisplayName("verifyResult: throws if order is not RESULT_ENTERED")
    void verifyResult_wrongStatus_throws() {
        savedOrder.setStatus(TestOrder.TestOrderStatus.ORDERED);
        when(testOrderRepository.findById(100L)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> service.verifyResult(100L))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("RESULT_ENTERED");
    }

    // ── cancelOrder ───────────────────────────────────────────────

    @Test
    @DisplayName("cancelOrder: soft-deletes order and notifies patient")
    void cancelOrder_success() {
        when(testOrderRepository.findById(100L)).thenReturn(Optional.of(savedOrder));
        when(testOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cancelOrder(100L);

        verify(testOrderRepository).save(argThat(o ->
            o.getStatus() == TestOrder.TestOrderStatus.CANCELLED && o.isDeleted()));
        verify(notificationService).send(
            eq(1L), anyString(), eq(NotificationLog.NotificationType.ORDER_CANCELLED));
    }

    @Test
    @DisplayName("cancelOrder: throws BusinessRuleException for COMPLETED orders")
    void cancelOrder_completed_throws() {
        savedOrder.setStatus(TestOrder.TestOrderStatus.COMPLETED);
        when(testOrderRepository.findById(100L)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> service.cancelOrder(100L))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("COMPLETED");
    }

    // ── detectAbnormal (ported from CLI v1) ───────────────────────

    @ParameterizedTest(name = "value={0}, range={1} → abnormal={2}")
    @CsvSource({
        "350, 70-100, true",
        "50,  70-100, true",
        "85,  70-100, false",
        "6.2, < 5.6,  true",
        "4.0, < 5.6,  false",
        "1.0, > 2.0,  true",
        "3.5, > 2.0,  false",
        "Negative, Negative, false"
    })
    @DisplayName("detectAbnormal: reference range comparisons")
    void detectAbnormal_rangeTests(String value, String range, boolean expected) {
        assertThat(TestOrderService.detectAbnormal(value, range)).isEqualTo(expected);
    }
}
