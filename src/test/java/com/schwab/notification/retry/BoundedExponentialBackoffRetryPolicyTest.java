package com.schwab.notification.retry;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class BoundedExponentialBackoffRetryPolicyTest {

    private final RetryPolicy policy = new BoundedExponentialBackoffRetryPolicy();

    @Test
    void delayDoublesEachAttemptUpToTheCap() {
        assertThat(policy.nextDelay(1)).isEqualTo(Duration.ofSeconds(2));
        assertThat(policy.nextDelay(2)).isEqualTo(Duration.ofSeconds(4));
        assertThat(policy.nextDelay(3)).isEqualTo(Duration.ofSeconds(8));
        assertThat(policy.nextDelay(4)).isEqualTo(Duration.ofSeconds(16));
        assertThat(policy.nextDelay(5)).isEqualTo(Duration.ofSeconds(32));
    }

    @Test
    void delayNeverExceedsTheCapEvenPastMaxAttempts() {
        assertThat(policy.nextDelay(10)).isEqualTo(Duration.ofSeconds(32));
    }

    @Test
    void maxAttemptsIsBounded() {
        assertThat(policy.maxAttempts()).isEqualTo(5);
    }
}
