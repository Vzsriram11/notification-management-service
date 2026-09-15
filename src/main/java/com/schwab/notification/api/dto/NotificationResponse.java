package com.schwab.notification.api.dto;

import com.schwab.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

/** Response for a successful (or deduplicated) submission. */
public record NotificationResponse(
        UUID notificationId,
        NotificationStatus status,
        boolean deduplicated, // true when this call matched an existing idempotency key (requirement 4.4)
        Instant createdAt
) {}
