package com.schwab.notification.retry;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Requirement 4.5 — a transient provider hiccup, a timeout, or a rate limit
 * is worth another attempt; a permanent rejection, an invalid recipient, or
 * an auth failure won't fix itself by retrying, so those go straight to
 * FAILED instead of burning through the retry budget.
 */
@Component
public class DefaultFailureClassifier implements FailureClassifier {

    private static final Set<DeliveryFailureType> RETRYABLE = Set.of(
            DeliveryFailureType.TRANSIENT_PROVIDER_FAILURE,
            DeliveryFailureType.TIMEOUT,
            DeliveryFailureType.RATE_LIMITED
    );

    @Override
    public boolean isRetryable(DeliveryFailureType type) {
        return type != null && RETRYABLE.contains(type);
    }
}
