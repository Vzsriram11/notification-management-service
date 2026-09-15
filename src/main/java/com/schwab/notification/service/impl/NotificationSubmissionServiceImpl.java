package com.schwab.notification.service.impl;

import com.schwab.notification.api.dto.NotificationRequest;
import com.schwab.notification.api.dto.NotificationResponse;
import com.schwab.notification.domain.*;
import com.schwab.notification.repository.DeliveryAttemptRepository;
import com.schwab.notification.repository.NotificationRepository;
import com.schwab.notification.repository.RecipientRepository;
import com.schwab.notification.service.AuditService;
import com.schwab.notification.service.ChannelRoutingService;
import com.schwab.notification.service.NotificationSubmissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationSubmissionServiceImpl implements NotificationSubmissionService {

    private final NotificationRepository notificationRepository;
    private final RecipientRepository recipientRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final ChannelRoutingService routingService;
    private final AuditService auditService;

    public NotificationSubmissionServiceImpl(NotificationRepository notificationRepository,
                                              RecipientRepository recipientRepository,
                                              DeliveryAttemptRepository deliveryAttemptRepository,
                                              ChannelRoutingService routingService,
                                              AuditService auditService) {
        this.notificationRepository = notificationRepository;
        this.recipientRepository = recipientRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.routingService = routingService;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public NotificationResponse submit(NotificationRequest request) {
        // Requirement 4.4 — the same idempotency key must not create a second logical notification.
        Optional<Notification> existing = notificationRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            Notification n = existing.get();
            auditService.record(n.getId(), null, "NOTIFICATION_DEDUPLICATED",
                    "Resubmission with existing idempotency key");
            return new NotificationResponse(n.getId(), n.getStatus(), true, n.getCreatedAt());
        }

        Notification notification = new Notification(
                UUID.randomUUID(),
                request.idempotencyKey(),
                request.sourceSystem(),
                request.eventId(),
                request.notificationType(),
                request.severity(),
                request.priority(),
                NotificationStatus.RECEIVED,
                request.requestedChannels(),
                Instant.now(),
                request.scheduledAt(),
                request.expiresAt()
        );
        notificationRepository.save(notification);
        auditService.record(notification.getId(), null, "NOTIFICATION_ACCEPTED",
                "source=" + request.sourceSystem() + " type=" + request.notificationType());

        for (String recipientRef : request.recipientRefs()) {
            Recipient recipient = recipientRepository.findByExternalRef(recipientRef)
                    .orElseGet(() -> recipientRepository.save(
                            new Recipient(UUID.randomUUID(), recipientRef, List.of())));

            List<Channel> channels = routingService.resolveChannels(notification, recipient);
            for (Channel channel : channels) {
                DeliveryAttempt attempt = new DeliveryAttempt(
                        UUID.randomUUID(), notification, recipient, channel,
                        DeliveryAttemptStatus.PENDING, 0, Instant.now(), null, Instant.now());
                deliveryAttemptRepository.save(attempt);
                auditService.record(notification.getId(), attempt.getId(), channel, "DELIVERY_QUEUED",
                        "recipient=" + recipientRef);
            }
        }

        notification.setStatus(NotificationStatus.ROUTED);
        notificationRepository.save(notification);
        auditService.record(notification.getId(), null, "ROUTING_DECIDED", "delivery attempts queued");

        return new NotificationResponse(notification.getId(), notification.getStatus(), false, notification.getCreatedAt());
    }
}
