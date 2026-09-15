package com.schwab.notification.provider;

import com.schwab.notification.retry.DeliveryFailureType;

/**
 * Result of one provider send attempt. success=true means delivered;
 * otherwise failureType drives FailureClassifier's retry/no-retry decision.
 */
public record DeliveryOutcome(boolean success, DeliveryFailureType failureType, String detail) {
    public static DeliveryOutcome ok() {
        return new DeliveryOutcome(true, null, null);
    }
    public static DeliveryOutcome failure(DeliveryFailureType type, String detail) {
        return new DeliveryOutcome(false, type, detail);
    }
}
