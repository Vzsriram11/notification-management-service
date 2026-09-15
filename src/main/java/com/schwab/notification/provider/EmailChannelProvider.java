package com.schwab.notification.provider;

import com.schwab.notification.domain.Channel;
import com.schwab.notification.domain.DeliveryAttempt;
import org.springframework.stereotype.Component;

/**
 * Simulated provider — no real email is sent. Wire this to
 * ProviderSimulationProperties so success/failure/timeout rates are
 * configurable, which makes the retry paths testable on demand instead
 * of by luck.
 */
@Component
public class EmailChannelProvider implements NotificationChannelProvider {

    @Override
    public Channel supportedChannel() {
        return Channel.EMAIL;
    }

    @Override
    public DeliveryOutcome send(DeliveryAttempt attempt) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
    }
}
