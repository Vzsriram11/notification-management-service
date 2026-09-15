package com.schwab.notification.provider;

import com.schwab.notification.domain.Channel;
import com.schwab.notification.domain.DeliveryAttempt;
import com.schwab.notification.domain.DeliveryAttemptStatus;
import com.schwab.notification.domain.DevicePlatform;
import com.schwab.notification.domain.Recipient;
import com.schwab.notification.retry.DeliveryFailureType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies PushChannelProvider's platform dispatch on its own — no need to
 * wait on DeliveryWorker (still a no-op; that's Session B) to prove this
 * logic is correct.
 */
class PushChannelProviderTest {

    private DeliveryAttempt attemptFor(Recipient recipient) {
        return new DeliveryAttempt(UUID.randomUUID(), null, recipient, Channel.PUSH,
                DeliveryAttemptStatus.PENDING, 0, Instant.now(), null, Instant.now());
    }

    @Test
    void dispatchesIosRecipientsToApns() {
        StubSender apns = new StubSender(DevicePlatform.IOS, DeliveryOutcome.ok());
        StubSender fcm = new StubSender(DevicePlatform.ANDROID, DeliveryOutcome.ok());
        PushChannelProvider provider = new PushChannelProvider(List.of(apns, fcm));

        Recipient iosRecipient = new Recipient(UUID.randomUUID(), "user-1", List.of(), DevicePlatform.IOS);
        DeliveryOutcome outcome = provider.send(attemptFor(iosRecipient));

        assertThat(outcome.success()).isTrue();
        assertThat(apns.invoked).isTrue();
        assertThat(fcm.invoked).isFalse();
    }

    @Test
    void dispatchesAndroidRecipientsToFcm() {
        StubSender apns = new StubSender(DevicePlatform.IOS, DeliveryOutcome.ok());
        StubSender fcm = new StubSender(DevicePlatform.ANDROID, DeliveryOutcome.ok());
        PushChannelProvider provider = new PushChannelProvider(List.of(apns, fcm));

        Recipient androidRecipient = new Recipient(UUID.randomUUID(), "user-2", List.of(), DevicePlatform.ANDROID);
        DeliveryOutcome outcome = provider.send(attemptFor(androidRecipient));

        assertThat(outcome.success()).isTrue();
        assertThat(fcm.invoked).isTrue();
        assertThat(apns.invoked).isFalse();
    }

    @Test
    void failsWithInvalidRecipientWhenNoDevicePlatformIsOnFile() {
        PushChannelProvider provider = new PushChannelProvider(List.of());

        Recipient noDeviceRecipient = new Recipient(UUID.randomUUID(), "user-3", List.of(), null);
        DeliveryOutcome outcome = provider.send(attemptFor(noDeviceRecipient));

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.failureType()).isEqualTo(DeliveryFailureType.INVALID_RECIPIENT);
    }

    /** Local stub — keeps this test focused on dispatch logic, no mocking framework needed. */
    private static class StubSender implements PlatformPushSender {
        private final DevicePlatform platform;
        private final DeliveryOutcome outcome;
        boolean invoked = false;

        StubSender(DevicePlatform platform, DeliveryOutcome outcome) {
            this.platform = platform;
            this.outcome = outcome;
        }

        @Override
        public DevicePlatform supportedPlatform() {
            return platform;
        }

        @Override
        public DeliveryOutcome send(DeliveryAttempt attempt) {
            invoked = true;
            return outcome;
        }
    }
}
