package com.cognizant.medlab.integration;

import java.math.BigDecimal;

/**
 * Payment gateway abstraction.
 *
 * v2: MockPaymentGateway — always succeeds, returns MOCK-XXXXXXXX ref.
 * v3: Replace with RazorpayGateway / StripeGateway — single class swap,
 *     no changes needed in BillingService.
 */
public interface PaymentGateway {

    /**
     * Initiates a payment charge.
     *
     * @param invoiceNumber invoice being paid
     * @param amount        amount to charge
     * @param method        payment method (CARD, UPI, etc.)
     * @return              gateway transaction reference
     * @throws PaymentGatewayException if the charge fails
     */
    String charge(String invoiceNumber, BigDecimal amount, String method);

    /**
     * Initiates a refund for a previous charge.
     *
     * @param providerRef   original transaction reference
     * @param amount        amount to refund
     * @return              refund reference
     */
    String refund(String providerRef, BigDecimal amount);
}
