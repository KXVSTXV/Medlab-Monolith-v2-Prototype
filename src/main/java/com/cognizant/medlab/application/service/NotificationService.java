package com.cognizant.medlab.application.service;

import com.cognizant.medlab.domain.notification.NotificationLog;
import com.cognizant.medlab.domain.notification.NotificationLog.NotificationChannel;
import com.cognizant.medlab.domain.notification.NotificationLog.NotificationType;
import com.cognizant.medlab.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Notification Service — core of Project 10's mandatory Notification Service.
 *
 * v2 delivery: persists to notification_logs table + SLF4J console output.
 *
 * Upgrade path v2→v3:
 *   Inject a NotificationChannel interface (EmailChannel, SmsChannel) and
 *   dispatch based on patient preferences. No changes to callers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    // ── Send ─────────────────────────────────────────────────────

    /**
     * Sends a notification asynchronously — fire-and-forget (best-effort).
     * A DB failure here must NOT roll back the parent business transaction.
     */
    @Async
    @Transactional
    public void send(Long patientId, String message, NotificationType type) {
        sendSync(patientId, null, message, type, NotificationChannel.SYSTEM);
    }

    @Async
    @Transactional
    public void sendToUser(Long userId, String message, NotificationType type) {
        sendSync(null, userId, message, type, NotificationChannel.SYSTEM);
    }

    /** Synchronous variant — used inside the same transaction by callers that need it. */
    @Transactional
    public NotificationLog sendSync(Long patientId, Long userId, String message,
                                    NotificationType type, NotificationChannel channel) {
        NotificationLog n = NotificationLog.builder()
            .patientId(patientId)
            .userId(userId)
            .message(message)
            .type(type)
            .channel(channel)
            .deliveryStatus(NotificationLog.DeliveryStatus.SENT)
            .build();

        NotificationLog saved = notificationLogRepository.save(n);

        // Console delivery — v3: replace with email/SMS dispatch
        log.info("╔══ NOTIFICATION [{}] ══════════════════════╗", type);
        log.info("║  Patient  : {}", patientId != null ? patientId : "—");
        log.info("║  Message  : {}", message);
        log.info("╚════════════════════════════════════════════╝");

        return saved;
    }

    // ── Query ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationLog> getForPatient(Long patientId) {
        return notificationLogRepository.findByPatientId(patientId);
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> getUnreadForPatient(Long patientId) {
        return notificationLogRepository.findUnreadByPatientId(patientId);
    }

    @Transactional(readOnly = true)
    public Page<NotificationLog> getAll(Pageable pageable) {
        return notificationLogRepository.findAllActive(pageable);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationLogRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationLogRepository.save(n);
        });
    }
}
