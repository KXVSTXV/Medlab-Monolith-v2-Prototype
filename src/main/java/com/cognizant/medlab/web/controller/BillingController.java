package com.cognizant.medlab.web.controller;

import com.cognizant.medlab.application.service.BillingService;
import com.cognizant.medlab.domain.billing.*;
import com.cognizant.medlab.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Invoice and payment management")
public class BillingController {

    private final BillingService billingService;

    // ── Invoices ──────────────────────────────────────────────────

    @PostMapping("/invoices/appointment/{appointmentId}")
    @Operation(summary = "Create invoice for an appointment (BILLING, RECEPTION, ADMIN)")
    public ResponseEntity<Invoice> createInvoice(@PathVariable Long appointmentId) {
        return ResponseEntity.status(201)
            .body(billingService.createInvoice(appointmentId));
    }

    @GetMapping("/invoices")
    @Operation(summary = "List all invoices (paged)")
    public ResponseEntity<PageResponse<Invoice>> listInvoices(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
            billingService.findAllInvoices(PageRequest.of(page, size))));
    }

    @GetMapping("/invoices/{id}")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<Invoice> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.findInvoiceById(id));
    }

    @GetMapping("/invoices/patient/{patientId}")
    @Operation(summary = "List invoices by patient")
    public ResponseEntity<PageResponse<Invoice>> invoicesByPatient(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
            billingService.findInvoicesByPatient(patientId, PageRequest.of(page, size))));
    }

    // ── Payments ──────────────────────────────────────────────────

    @PostMapping("/invoices/{invoiceId}/pay")
    @Operation(summary = "Record a payment for an invoice (BILLING, RECEPTION, ADMIN)")
    public ResponseEntity<Payment> pay(
            @PathVariable Long invoiceId,
            @Valid @RequestBody OperationsDto.PaymentRequest req) {
        return ResponseEntity.status(201).body(
            billingService.recordPayment(invoiceId, req.amount(), req.method()));
    }

    @GetMapping("/invoices/{invoiceId}/payments")
    @Operation(summary = "List payments for an invoice")
    public ResponseEntity<List<Payment>> paymentsByInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(billingService.findPaymentsByInvoice(invoiceId));
    }
}
