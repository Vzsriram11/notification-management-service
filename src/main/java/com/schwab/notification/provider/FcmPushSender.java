package com.schwab.notification.provider;

import com.schwab.notification.config.ProviderSimulationProperties;
import com.schwab.notification.domain.DeliveryAttempt;
import com.schwab.notification.domain.DevicePlatform;
import com.schwab.notification.retry.DeliveryFailureType;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/** Simulated FCM sender — see EmailChannelProvider for the pattern. */
@Component
public class FcmPushSender implements PlatformPushSender {

    private final ProviderSimulationProperties simulation;

    public FcmPushSender(ProviderSimulationProperties simulation) {
        this.simulation = simulation;
    }

    @Override
    public DevicePlatform supportedPlatform() {
        return DevicePlatform.ANDROID;
    }

    @Override
    public DeliveryOutcome send(DeliveryAttempt attempt) {
        double roll = ThreadLocalRandom.current().nextDouble();
        String recipientRef = attempt.getRecipient().getExternalRef();

        if (roll < simulation.getTimeoutRate()) {
            return DeliveryOutcome.failure(DeliveryFailureType.TIMEOUT,
                    "simulated FCM timeout for " + recipientRef);
        }
        if (roll < simulation.getTimeoutRate() + simulation.getFailureRate()) {
            return DeliveryOutcome.failure(DeliveryFailureType.TRANSIENT_PROVIDER_FAILURE,
                    "simulated FCM failure for " + recipientRef);
        }
        return DeliveryOutcome.ok();
    }
}
