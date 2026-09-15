package com.schwab.notification.service;

import com.schwab.notification.domain.Channel;
import java.util.UUID;

/** Requirement 4.9. A thin write API so every layer records events the same way. */
public interface AuditService {

    /** Notification-level event with no specific channel — e.g. accepted, routed. */
    void record(UUID notificationId, UUID deliveryAttemptId, String eventType, String detail);

    /** Attempt-level event tied to a specific channel — e.g. queued, attempted, succeeded, failed. */
    void record(UUID notificationId, UUID deliveryAttemptId, Channel channel, String eventType, String detail);
}
