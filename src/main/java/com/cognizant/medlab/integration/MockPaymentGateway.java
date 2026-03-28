package com.cognizant.medlab.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ⚠️  MOCK IMPLEMENTATION — for development and testing only.
 *
 * Always succeeds. Returns a deterministic MOCK-XXXXXXXX transaction reference.
 *
 * To swap in a real gateway (v3):
 *   1. Add Razorpay/Stripe SDK dependency to pom.xml.
 *   2. Create RazorpayGateway implements PaymentGateway.
 *   3. Annotate MockPaymentGateway with @Profile("!prod") and the real one with @Profile("prod").
 *   4. No changes required in BillingService.
 */
@Slf4j
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public String charge(String invoiceNumber, BigDecimal amount, String method) {
        String ref = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[MOCK PaymentGateway] Charged {} {} for invoice {} → ref: {}",
                 amount, method, invoiceNumber, ref);
        return ref;
    }

    @Override
    public String refund(String providerRef, BigDecimal amount) {
        String refundRef = "REFUND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[MOCK PaymentGateway] Refunded {} for original ref {} → refund ref: {}",
                 amount, providerRef, refundRef);
        return refundRef;
    }
}
