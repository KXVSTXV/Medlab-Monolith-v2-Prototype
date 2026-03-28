package com.cognizant.medlab.web.dto;

import com.cognizant.medlab.domain.processing.TestOrder;
import com.cognizant.medlab.domain.scheduling.Sample;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Misc request DTOs for scheduling, processing and billing. */
public final class OperationsDto {

    private OperationsDto() {}

    // ── Appointment ───────────────────────────────────────────────

    public record AppointmentCreateRequest(
        @NotNull(message = "Patient ID is required") Long patientId,
        @NotNull(message = "Scheduled time is required")
        @Future(message = "Scheduled time must be in the future")
        LocalDateTime scheduledAt,
        String location,
        String notes
    ) {}

    // ── Sample ────────────────────────────────────────────────────

    public record SampleCollectRequest(
        @NotNull(message = "Appointment ID is required") Long appointmentId,
        @NotNull(message = "Specimen type is required") Sample.SpecimenType specimenType
    ) {}

    public record SampleStatusRequest(
        @NotNull Sample.SampleStatus status,
        String rejectionReason
    ) {}

    // ── Test Order ────────────────────────────────────────────────

    public record TestOrderCreateRequest(
        @NotNull(message = "Sample ID is required") Long sampleId,
        @NotNull(message = "Lab test ID is required") Long labTestId,
        @NotNull TestOrder.Priority priority,
        String notes
    ) {}

    public record TestResultSubmitRequest(
        @NotBlank(message = "Result value is required") String value,
        String unit,
        String referenceRange,
        String remarks
    ) {}

    // ── Billing ───────────────────────────────────────────────────

    public record PaymentRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Payment method is required")
        com.cognizant.medlab.domain.billing.Payment.PaymentMethod method
    ) {}

    // ── Lab Test Catalog ──────────────────────────────────────────

    public record LabTestCreateRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        String specimenType,
        Integer turnaroundHours,
        @DecimalMin("0.0") BigDecimal price,
        String referenceRange
    ) {}

    public record PanelCreateRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotEmpty java.util.Set<Long> testIds
    ) {}
}
