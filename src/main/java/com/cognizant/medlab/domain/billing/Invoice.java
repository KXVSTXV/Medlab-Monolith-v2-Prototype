package com.cognizant.medlab.domain.billing;

import com.cognizant.medlab.domain.common.BaseEntity;
import com.cognizant.medlab.domain.patient.Patient;
import com.cognizant.medlab.domain.scheduling.Appointment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Invoice — billing record linked to a patient appointment.
 *
 * Status lifecycle: PENDING → PAID | OVERDUE | CANCELLED
 */
@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"patient", "appointment"})
@EqualsAndHashCode(callSuper = false, of = "invoiceNumber")
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", unique = true, nullable = false, length = 100)
    private String invoiceNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String lineItems;   // JSON string of line items (test name → price)

    // ── Enum ─────────────────────────────────────────────────────

    public enum InvoiceStatus {
        PENDING,
        PAID,
        OVERDUE,
        CANCELLED,
        PARTIALLY_PAID
    }
}
