package com.schwab.notification.service;

import com.schwab.notification.api.dto.NotificationStatusResponse;
import java.util.UUID;

/** Read model for requirement 4.2 — assembles a Notification and its DeliveryAttempts. */
public interface NotificationStatusService {
    NotificationStatusResponse getStatus(UUID notificationId);
}
