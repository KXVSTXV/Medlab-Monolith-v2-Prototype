package com.cognizant.medlab.domain.notification;

import com.cognizant.medlab.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * NotificationLog — every notification sent to a patient is persisted here.
 *
 * v2 delivery: SLF4J console log (upgradeable to email/SMS in v3 via
 * a NotificationChannel interface with JavaMailSender / Twilio implementations).
 */
@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = false, of = "id")
public class NotificationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "user_id")
    private Long userId;          // recipient user id (if internal)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationChannel channel = NotificationChannel.SYSTEM;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.SENT;

    // ── Enums ────────────────────────────────────────────────────

    public enum NotificationType {
        ORDER_CREATED,
        SAMPLE_COLLECTED,
        RESULT_ENTERED,
        REPORT_READY,
        INVOICE_CREATED,
        PAYMENT_SUCCESS,
        APPOINTMENT_SCHEDULED,
        ORDER_CANCELLED,
        GENERAL
    }

    public enum NotificationChannel {
        SYSTEM,   // in-app
        EMAIL,    // v3: JavaMailSender
        SMS       // v3: Twilio
    }

    public enum DeliveryStatus {
        SENT,
        FAILED,
        PENDING
    }
}
