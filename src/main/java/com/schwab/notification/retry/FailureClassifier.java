package com.schwab.notification.retry;

/** Requirement 4.5 — maps a provider outcome to retryable vs terminal. */
public interface FailureClassifier {
    boolean isRetryable(DeliveryFailureType type);
}
