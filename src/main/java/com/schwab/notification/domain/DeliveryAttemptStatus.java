package com.schwab.notification.domain;

/** Per (recipient, channel) delivery attempt status — the fine-grained half of the requirement 4.2 status model. */
public enum DeliveryAttemptStatus {
    PENDING,
    IN_PROGRESS,
    SUCCEEDED,
    FAILED,
    RETRY_SCHEDULED,
    SUPPRESSED
}
