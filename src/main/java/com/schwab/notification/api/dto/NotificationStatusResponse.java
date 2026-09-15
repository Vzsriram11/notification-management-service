package com.schwab.notification.api.dto;

import com.schwab.notification.domain.NotificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Response for GET /notifications/{id}/status (requirement 4.2). */
public record NotificationStatusResponse(
        UUID notificationId,
        NotificationStatus overallStatus,
        List<String> selectedChannels,
        List<DeliveryStatusView> deliveries,
        Instant createdAt,
        Instant updatedAt
) {}
