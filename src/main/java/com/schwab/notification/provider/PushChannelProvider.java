package com.schwab.notification.provider;

import com.schwab.notification.domain.Channel;
import com.schwab.notification.domain.DeliveryAttempt;
import org.springframework.stereotype.Component;

/** Simulated provider — see EmailChannelProvider for the pattern. */
@Component
public class PushChannelProvider implements NotificationChannelProvider {

    @Override
    public Channel supportedChannel() {
        return Channel.PUSH;
    }

    @Override
    public DeliveryOutcome send(DeliveryAttempt attempt) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
    }
}
