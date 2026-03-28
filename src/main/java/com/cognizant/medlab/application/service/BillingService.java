package com.cognizant.medlab.application.service;

import com.cognizant.medlab.domain.billing.*;
import com.cognizant.medlab.domain.notification.NotificationLog.NotificationType;
import com.cognizant.medlab.domain.scheduling.Appointment;
import com.cognizant.medlab.exception.*;
import com.cognizant.medlab.integration.PaymentGateway;
import com.cognizant.medlab.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Billing Service — invoices, payments, refunds, reconciliation.
 *
 * Payment delegation: PaymentGateway interface (MockPaymentGateway in v2).
 * Swap to real Razorpay/Stripe in v3 without touching this class.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final InvoiceRepository     invoiceRepository;
    private final PaymentRepository     paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final TestOrderRepository   testOrderRepository;
    private final PaymentGateway        paymentGateway;
    private final NotificationService   notificationService;
    private final AuditService          auditService;

    // ── Create invoice ────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_BILLING','ROLE_RECEPTION')")
    public Invoice createInvoice(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .filter(a -> !a.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        if (invoiceRepository.findByAppointmentIdAndIsDeletedFalse(appointmentId).isPresent())
            throw new BusinessRuleException(
                "An invoice already exists for appointment #" + appointmentId);

        // Aggregate prices from all test orders for this appointment
        // Collect all test orders for the patient linked to this appointment
        List<com.cognizant.medlab.domain.processing.TestOrder> orders =
            testOrderRepository.findByPatientId(
                appointment.getPatient().getId(),
                org.springframework.data.domain.PageRequest.of(0, 200))
                .getContent();

        BigDecimal subtotal = orders.stream()
            .map(o -> o.getLabTest().getPrice() != null
                      ? o.getLabTest().getPrice() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tax = subtotal.multiply(new BigDecimal("0.18")); // 18% GST
        BigDecimal total = subtotal.add(tax);

        String lineItemsJson = buildLineItemsJson(orders);

        Invoice invoice = Invoice.builder()
            .invoiceNumber(generateInvoiceNumber())
            .patient(appointment.getPatient())
            .appointment(appointment)
            .amount(total)
            .taxAmount(tax)
            .dueDate(LocalDate.now().plusDays(30))
            .status(Invoice.InvoiceStatus.PENDING)
            .lineItems(lineItemsJson)
            .build();

        Invoice saved = invoiceRepository.save(invoice);

        notificationService.send(appointment.getPatient().getId(),
            "Invoice " + saved.getInvoiceNumber() + " for ₹" + total
                + " has been created. Due: " + saved.getDueDate(),
            NotificationType.INVOICE_CREATED);

        auditService.log("INVOICE_CREATED", "Invoice", saved.getId(),
                         "number=" + saved.getInvoiceNumber() + ", amount=" + total);
        return saved;
    }

    // ── Record payment ────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_BILLING','ROLE_RECEPTION')")
    public Payment recordPayment(Long invoiceId, BigDecimal amount,
                                 Payment.PaymentMethod method) {
        Invoice invoice = findInvoiceById(invoiceId);

        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID)
            throw new BusinessRuleException("Invoice #" + invoiceId + " is already PAID.");
        if (invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED)
            throw new BusinessRuleException("Cannot pay a CANCELLED invoice.");

        // Delegate to payment gateway (mock in v2)
        String providerRef = paymentGateway.charge(
            invoice.getInvoiceNumber(), amount, method.name());

        Payment payment = Payment.builder()
            .invoice(invoice)
            .amount(amount)
            .method(method)
            .providerRef(providerRef)
            .status(Payment.PaymentStatus.SUCCESS)
            .processedAt(LocalDateTime.now())
            .build();

        Payment saved = paymentRepository.save(payment);

        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        notificationService.send(invoice.getPatient().getId(),
            "Payment of ₹" + amount + " received for invoice "
                + invoice.getInvoiceNumber() + ". Ref: " + providerRef,
            NotificationType.PAYMENT_SUCCESS);

        auditService.log("PAYMENT_RECORDED", "Payment", saved.getId(),
                         "invoice=" + invoice.getInvoiceNumber()
                         + ", amount=" + amount + ", ref=" + providerRef);
        return saved;
    }

    // ── Query ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Invoice findInvoiceById(Long id) {
        return invoiceRepository.findById(id)
            .filter(i -> !i.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Invoice> findAllInvoices(Pageable pageable) {
        return invoiceRepository.findAllActive(pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Invoice> findInvoicesByPatient(Long patientId, Pageable pageable) {
        return invoiceRepository.findByPatientId(patientId, pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<Payment> findPaymentsByInvoice(Long invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String generateInvoiceNumber() {
        return "INV-" + System.currentTimeMillis();
    }

    private String buildLineItemsJson(
            List<com.cognizant.medlab.domain.processing.TestOrder> orders) {
        return orders.stream()
            .map(o -> "\"" + o.getLabTest().getName() + "\": "
                      + (o.getLabTest().getPrice() != null
                         ? o.getLabTest().getPrice() : "0"))
            .collect(Collectors.joining(", ", "{", "}"));
    }
}
