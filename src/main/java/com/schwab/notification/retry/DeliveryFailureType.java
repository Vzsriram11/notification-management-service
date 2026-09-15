package com.schwab.notification.retry;

/** Failure taxonomy required by requirement 4.5 — drives whether FailureClassifier marks an attempt retryable. */
public enum DeliveryFailureType {
    TRANSIENT_PROVIDER_FAILURE,
    PERMANENT_PROVIDER_REJECTION,
    INVALID_RECIPIENT,
    RATE_LIMITED,
    TIMEOUT,
    AUTH_ERROR
}
