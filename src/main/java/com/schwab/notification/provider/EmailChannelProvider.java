package com.schwab.notification.provider;

import com.schwab.notification.config.ProviderSimulationProperties;
import com.schwab.notification.domain.Channel;
import com.schwab.notification.domain.DeliveryAttempt;
import com.schwab.notification.retry.DeliveryFailureType;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulated provider — no real email is sent. Failure/timeout rates come
 * from ProviderSimulationProperties (notification.simulation.* in
 * application.yml) so every branch of the retry logic (requirement 4.5)
 * can be exercised on demand instead of by waiting for a real outage.
 */
@Component
public class EmailChannelProvider implements NotificationChannelProvider {

    private final ProviderSimulationProperties simulation;

    public EmailChannelProvider(ProviderSimulationProperties simulation) {
        this.simulation = simulation;
    }

    @Override
    public Channel supportedChannel() {
        return Channel.EMAIL;
    }

    @Override
    public DeliveryOutcome send(DeliveryAttempt attempt) {
        double roll = ThreadLocalRandom.current().nextDouble();
        String recipientRef = attempt.getRecipient().getExternalRef();

        if (roll < simulation.getTimeoutRate()) {
            return DeliveryOutcome.failure(DeliveryFailureType.TIMEOUT,
                    "simulated timeout sending email to " + recipientRef);
        }
        if (roll < simulation.getTimeoutRate() + simulation.getFailureRate()) {
            return DeliveryOutcome.failure(DeliveryFailureType.TRANSIENT_PROVIDER_FAILURE,
                    "simulated provider failure sending email to " + recipientRef);
        }
        return DeliveryOutcome.ok();
    }
}
