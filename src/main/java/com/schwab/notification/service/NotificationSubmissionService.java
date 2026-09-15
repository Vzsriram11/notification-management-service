package com.schwab.notification.service;

import com.schwab.notification.api.dto.NotificationRequest;
import com.schwab.notification.api.dto.NotificationResponse;

/**
 * Accepts and persists a notification (requirement 4.1), including the
 * idempotency check (requirement 4.4): look up by idempotencyKey first;
 * if found, return the existing record (deduplicated = true) instead of
 * creating a second logical notification.
 */
public interface NotificationSubmissionService {
    NotificationResponse submit(NotificationRequest request);
}
