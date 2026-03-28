package com.cognizant.medlab.domain.billing;

import com.cognizant.medlab.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment — a single payment transaction against an Invoice.
 *
 * providerRef is populated by the PaymentGateway adapter (mock in v2).
 * In v3, this is the Razorpay/Stripe transaction ID.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "invoice")
@EqualsAndHashCode(callSuper = false, of = "id")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentMethod method = PaymentMethod.CASH;

    @Column(name = "provider_ref", length = 255)
    private String providerRef;     // gateway transaction ID (mock: "MOCK-XXXXXXXX")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(length = 255)
    private String remarks;

    // ── Enums ────────────────────────────────────────────────────

    public enum PaymentMethod {
        CASH, CARD, UPI, NET_BANKING, CHEQUE
    }

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED, REFUNDED
    }
}
