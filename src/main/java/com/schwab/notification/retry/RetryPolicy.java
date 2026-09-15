package com.schwab.notification.retry;

import java.time.Duration;

/** Bounded exponential backoff (requirement 4.5). Keep the schedule short and documented. */
public interface RetryPolicy {
    int maxAttempts();
    Duration nextDelay(int attemptCount);
}
