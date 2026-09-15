package com.schwab.notification.domain;

/**
 * Overall notification status (requirement 4.2). Custom state model — the
 * assessment explicitly allows a documented alternative to a default one.
 */
public enum NotificationStatus {
    RECEIVED,
    ROUTED,
    PROCESSING,
    PARTIALLY_DELIVERED,
    DELIVERED,
    FAILED,
    EXPIRED,
    SUPPRESSED // duplicate or deduplicated — requirement 4.4
}
