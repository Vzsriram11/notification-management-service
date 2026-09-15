package com.schwab.notification.provider;

import com.schwab.notification.domain.DeliveryAttempt;
import com.schwab.notification.domain.DevicePlatform;

/**
 * One implementation per push platform (APNs, FCM). PushChannelProvider
 * dispatches to whichever of these matches the recipient's DevicePlatform —
 * the same idea ProviderRegistry documents at the Channel level (a
 * List<Interface> constructor param auto-collecting all matching beans),
 * applied one level down, within the PUSH channel.
 */
public interface PlatformPushSender {
    DevicePlatform supportedPlatform();
    DeliveryOutcome send(DeliveryAttempt attempt);
}
