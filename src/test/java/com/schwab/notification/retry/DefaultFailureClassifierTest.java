package com.schwab.notification.retry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFailureClassifierTest {

    private final FailureClassifier classifier = new DefaultFailureClassifier();

    @Test
    void transientTimeoutAndRateLimitedAreRetryable() {
        assertThat(classifier.isRetryable(DeliveryFailureType.TRANSIENT_PROVIDER_FAILURE)).isTrue();
        assertThat(classifier.isRetryable(DeliveryFailureType.TIMEOUT)).isTrue();
        assertThat(classifier.isRetryable(DeliveryFailureType.RATE_LIMITED)).isTrue();
    }

    @Test
    void permanentRejectionInvalidRecipientAndAuthErrorAreNotRetryable() {
        assertThat(classifier.isRetryable(DeliveryFailureType.PERMANENT_PROVIDER_REJECTION)).isFalse();
        assertThat(classifier.isRetryable(DeliveryFailureType.INVALID_RECIPIENT)).isFalse();
        assertThat(classifier.isRetryable(DeliveryFailureType.AUTH_ERROR)).isFalse();
    }
}
