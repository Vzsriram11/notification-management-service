package com.schwab.notification.provider;

import com.schwab.notification.domain.Channel;
import com.schwab.notification.domain.DeliveryAttempt;
import com.schwab.notification.domain.DevicePlatform;
import com.schwab.notification.domain.Recipient;
import com.schwab.notification.retry.DeliveryFailureType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PUSH isn't one wire protocol — iOS goes through APNs, Android through
 * FCM, and the two have different payload shapes, auth models, and error
 * semantics. Branching on platform inside a single provider is where that
 * would start leaking together, so this class stays a thin dispatcher and
 * delegates to whichever PlatformPushSender matches the recipient's
 * registered device. Adding a third push platform later means adding a new
 * PlatformPushSender bean, not editing this class or the worker.
 */
@Component
public class PushChannelProvider implements NotificationChannelProvider {

    private final Map<DevicePlatform, PlatformPushSender> sendersByPlatform;

    public PushChannelProvider(List<PlatformPushSender> senders) {
        this.sendersByPlatform = senders.stream()
                .collect(Collectors.toMap(PlatformPushSender::supportedPlatform, Function.identity()));
    }

    @Override
    public Channel supportedChannel() {
        return Channel.PUSH;
    }

    @Override
    public DeliveryOutcome send(DeliveryAttempt attempt) {
        Recipient recipient = attempt.getRecipient();
        DevicePlatform platform = recipient.getDevicePlatform();

        if (platform == null) {
            return DeliveryOutcome.failure(DeliveryFailureType.INVALID_RECIPIENT,
                    "recipient " + recipient.getExternalRef() + " has no registered device platform");
        }

        PlatformPushSender sender = sendersByPlatform.get(platform);
        if (sender == null) {
            return DeliveryOutcome.failure(DeliveryFailureType.INVALID_RECIPIENT,
                    "no push sender configured for platform " + platform);
        }

        return sender.send(attempt);
    }
}
