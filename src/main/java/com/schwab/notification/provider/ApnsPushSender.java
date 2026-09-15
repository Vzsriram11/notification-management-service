package com.schwab.notification.provider;

import com.schwab.notification.config.ProviderSimulationProperties;
import com.schwab.notification.domain.DeliveryAttempt;
import com.schwab.notification.domain.DevicePlatform;
import com.schwab.notification.retry.DeliveryFailureType;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/** Simulated APNs sender — see EmailChannelProvider for the pattern. */
@Component
public class ApnsPushSender implements PlatformPushSender {

    private final ProviderSimulationProperties simulation;

    public ApnsPushSender(ProviderSimulationProperties simulation) {
        this.simulation = simulation;
    }

    @Override
    public DevicePlatform supportedPlatform() {
        return DevicePlatform.IOS;
    }

    @Override
    public DeliveryOutcome send(DeliveryAttempt attempt) {
        double roll = ThreadLocalRandom.current().nextDouble();
        String recipientRef = attempt.getRecipient().getExternalRef();

        if (roll < simulation.getTimeoutRate()) {
            return DeliveryOutcome.failure(DeliveryFailureType.TIMEOUT,
                    "simulated APNs timeout for " + recipientRef);
        }
        if (roll < simulation.getTimeoutRate() + simulation.getFailureRate()) {
            return DeliveryOutcome.failure(DeliveryFailureType.TRANSIENT_PROVIDER_FAILURE,
                    "simulated APNs failure for " + recipientRef);
        }
        return DeliveryOutcome.ok();
    }
}
