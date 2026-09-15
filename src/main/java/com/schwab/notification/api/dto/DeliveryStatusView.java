package com.schwab.notification.api.dto;

import com.schwab.notification.domain.Channel;
import com.schwab.notification.domain.DeliveryAttemptStatus;
import java.time.Instant;

/** Per-recipient, per-channel line item within a status response (requirement 4.2). */
public record DeliveryStatusView(
        String recipientRef,
        Channel channel,
        DeliveryAttemptStatus status,
        int attemptCount,
        Instant lastAttemptedAt
) {}
