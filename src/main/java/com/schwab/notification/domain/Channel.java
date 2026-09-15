package com.schwab.notification.domain;

/**
 * Delivery channel. EMAIL/SMS/PUSH cover the initial build; dispatch goes through
 * NotificationChannelProvider so adding a new one doesn't touch existing channels.
 */
public enum Channel {
    EMAIL, SMS, PUSH
}
