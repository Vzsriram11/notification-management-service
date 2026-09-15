package com.schwab.notification.provider;

import com.schwab.notification.domain.Channel;
import com.schwab.notification.domain.DeliveryAttempt;

/**
 * One interface for every delivery channel, including ones added later —
 * a new channel means a new implementation, not a change to existing
 * dispatch logic.
 */
public interface NotificationChannelProvider {
    Channel supportedChannel();
    DeliveryOutcome send(DeliveryAttempt attempt);
}
