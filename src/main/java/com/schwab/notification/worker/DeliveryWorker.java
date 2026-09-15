package com.schwab.notification.worker;

import com.schwab.notification.domain.Channel;
import com.schwab.notification.domain.DeliveryAttempt;
import com.schwab.notification.domain.DeliveryAttemptStatus;
import com.schwab.notification.domain.Notification;
import com.schwab.notification.domain.NotificationStatus;
import com.schwab.notification.provider.DeliveryOutcome;
import com.schwab.notification.provider.NotificationChannelProvider;
import com.schwab.notification.repository.DeliveryAttemptRepository;
import com.schwab.notification.repository.NotificationRepository;
import com.schwab.notification.retry.DeliveryFailureType;
import com.schwab.notification.retry.FailureClassifier;
import com.schwab.notification.retry.RetryPolicy;
import com.schwab.notification.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Asynchronous processing for requirement 4.1, using an outbox-poller
 * design: finds PENDING/RETRY_SCHEDULED delivery attempts due now, claims
 * each atomically (DeliveryAttemptRepository.claim), dispatches to the
 * right provider, then records the outcome and an audit event.
 */
@Component
public class DeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(DeliveryWorker.class);

    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final NotificationRepository notificationRepository;
    private final Map<Channel, NotificationChannelProvider> providersByChannel;
    private final FailureClassifier failureClassifier;
    private final RetryPolicy retryPolicy;
    private final AuditService auditService;

    public DeliveryWorker(DeliveryAttemptRepository deliveryAttemptRepository,
                           NotificationRepository notificationRepository,
                           List<NotificationChannelProvider> providers,
                           FailureClassifier failureClassifier,
                           RetryPolicy retryPolicy,
                           AuditService auditService) {
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.notificationRepository = notificationRepository;
        this.providersByChannel = providers.stream()
                .collect(Collectors.toMap(NotificationChannelProvider::supportedChannel, Function.identity()));
        this.failureClassifier = failureClassifier;
        this.retryPolicy = retryPolicy;
        this.auditService = auditService;
    }

    @Scheduled(fixedDelayString = "${notification.worker.poll-interval-ms:2000}")
    public void pollAndDispatch() {
        Instant now = Instant.now();
        //pull the list of notifications that needs to be delivered
        List<DeliveryAttempt> due = new ArrayList<>();
        due.addAll(deliveryAttemptRepository.findByStatusAndNextAttemptAtBefore(DeliveryAttemptStatus.PENDING, now));
        due.addAll(deliveryAttemptRepository.findByStatusAndNextAttemptAtBefore(DeliveryAttemptStatus.RETRY_SCHEDULED, now));

        for (DeliveryAttempt attempt : due) {
            processOne(attempt);
        }
    }

    private void processOne(DeliveryAttempt attempt) {
        DeliveryAttemptStatus fromStatus = attempt.getStatus();
        int claimed = deliveryAttemptRepository.claim(attempt.getId(), fromStatus);
        if (claimed == 0) {
            // Another poller tick (or instance) already claimed this row — requirement 4.4:
            // no duplicate side effects, so we simply skip it this round.
            return;
        }

        DeliveryOutcome outcome = dispatch(attempt);

        attempt.setAttemptCount(attempt.getAttemptCount() + 1);
        attempt.setLastAttemptedAt(Instant.now());

        if (outcome.success()) {
            attempt.setStatus(DeliveryAttemptStatus.SUCCEEDED);
            deliveryAttemptRepository.save(attempt);
            auditService.record(attempt.getNotification().getId(), attempt.getId(), attempt.getChannel(),
                    "DELIVERY_SUCCEEDED", "attempt=" + attempt.getAttemptCount());
        } else {
            recordFailureOrRetry(attempt, outcome);
        }

        updateNotificationStatus(attempt.getNotification().getId());
    }

    private DeliveryOutcome dispatch(DeliveryAttempt attempt) {
        NotificationChannelProvider provider = providersByChannel.get(attempt.getChannel());
        if (provider == null) {
            return DeliveryOutcome.failure(DeliveryFailureType.PERMANENT_PROVIDER_REJECTION,
                    "no provider registered for channel " + attempt.getChannel());
        }
        try {
            return provider.send(attempt);
        } catch (Exception ex) {
            // An unexpected provider exception is treated as transient rather than
            // crashing the poller tick — one bad attempt should never block the rest.
            log.warn("Provider threw while sending attempt {}: {}", attempt.getId(), ex.getMessage());
            return DeliveryOutcome.failure(DeliveryFailureType.TRANSIENT_PROVIDER_FAILURE, ex.getMessage());
        }
    }

    private void recordFailureOrRetry(DeliveryAttempt attempt, DeliveryOutcome outcome) {
        boolean retryable = failureClassifier.isRetryable(outcome.failureType());
        boolean attemptsLeft = attempt.getAttemptCount() < retryPolicy.maxAttempts();

        if (retryable && attemptsLeft) {
            Duration delay = retryPolicy.nextDelay(attempt.getAttemptCount());
            attempt.setStatus(DeliveryAttemptStatus.RETRY_SCHEDULED);
            attempt.setNextAttemptAt(Instant.now().plus(delay));
            deliveryAttemptRepository.save(attempt);
            auditService.record(attempt.getNotification().getId(), attempt.getId(), attempt.getChannel(),
                    "DELIVERY_RETRY_SCHEDULED",
                    "attempt=" + attempt.getAttemptCount() + " reason=" + outcome.failureType()
                            + " nextAttemptAt=" + attempt.getNextAttemptAt());
        } else {
            attempt.setStatus(DeliveryAttemptStatus.FAILED);
            deliveryAttemptRepository.save(attempt);
            auditService.record(attempt.getNotification().getId(), attempt.getId(), attempt.getChannel(),
                    "DELIVERY_FAILED",
                    "attempt=" + attempt.getAttemptCount() + " reason=" + outcome.failureType()
                            + " detail=" + outcome.detail());
        }
    }

    /**
     * Recomputes the parent Notification's overall status (requirement 4.2)
     * from its delivery attempts. Reads through the repository rather than
     * Notification.getDeliveryAttempts() — that association is lazy and this
     * runs outside any single long-lived transaction, so walking it directly
     * here avoids a LazyInitializationException.
     */
    private void updateNotificationStatus(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            return;
        }

        List<DeliveryAttempt> attempts = deliveryAttemptRepository.findByNotification_Id(notificationId);
        if (attempts.isEmpty()) {
            return;
        }

        boolean anyInFlight = attempts.stream().anyMatch(a ->
                a.getStatus() == DeliveryAttemptStatus.PENDING
                        || a.getStatus() == DeliveryAttemptStatus.IN_PROGRESS
                        || a.getStatus() == DeliveryAttemptStatus.RETRY_SCHEDULED);
        boolean anySucceeded = attempts.stream().anyMatch(a -> a.getStatus() == DeliveryAttemptStatus.SUCCEEDED);
        boolean anyFailed = attempts.stream().anyMatch(a -> a.getStatus() == DeliveryAttemptStatus.FAILED);

        NotificationStatus newStatus;
        if (anyInFlight) {
            newStatus = NotificationStatus.PROCESSING;
        } else if (anySucceeded && anyFailed) {
            newStatus = NotificationStatus.PARTIALLY_DELIVERED;
        } else if (anySucceeded) {
            newStatus = NotificationStatus.DELIVERED;
        } else {
            newStatus = NotificationStatus.FAILED;
        }

        if (notification.getStatus() != newStatus) {
            notification.setStatus(newStatus);
            notificationRepository.save(notification);
            auditService.record(notification.getId(), null, "NOTIFICATION_STATUS_CHANGED", "status=" + newStatus);
        }
    }
}
