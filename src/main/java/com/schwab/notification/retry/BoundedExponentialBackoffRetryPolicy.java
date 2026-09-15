package com.schwab.notification.retry;

import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Bounded exponential backoff for requirement 4.5: 2s, 4s, 8s, 16s, 32s,
 * capped at 5 total attempts so a permanently-broken provider can't retry
 * forever. Numbers are deliberately small so the schedule is easy to watch
 * end-to-end in a short demo; a production version would externalize these
 * via ProviderSimulationProperties-style configuration.
 */
@Component
public class BoundedExponentialBackoffRetryPolicy implements RetryPolicy {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration BASE_DELAY = Duration.ofSeconds(2);
    private static final Duration MAX_DELAY = Duration.ofSeconds(32);

    @Override
    public int maxAttempts() {
        return MAX_ATTEMPTS;
    }

    @Override
    public Duration nextDelay(int attemptCount) {
        int exponent = Math.max(0, attemptCount - 1);
        long millis = BASE_DELAY.toMillis() * (1L << exponent);
        return Duration.ofMillis(Math.min(millis, MAX_DELAY.toMillis()));
    }
}
