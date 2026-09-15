package com.schwab.notification.domain;

/** Delivery priority (requirement 4.1). Distinct from severity: severity is "how bad", priority is "how fast". */
public enum Priority {
    LOW, NORMAL, HIGH, URGENT
}
