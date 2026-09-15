package com.schwab.notification.service.impl;

import com.schwab.notification.domain.Channel;
import com.schwab.notification.domain.Notification;
import com.schwab.notification.domain.NotificationStatus;
import com.schwab.notification.domain.Priority;
import com.schwab.notification.domain.Recipient;
import com.schwab.notification.domain.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the channel-routing precedence policy from the ambiguous-requirement
 * scenario (requirement 4.3 doesn't specify what wins when requested channel,
 * severity, and recipient preference disagree — see docs/scenario-ambiguous.md
 * for the full reasoning). One test per branch of ChannelRoutingServiceImpl.
 */
class ChannelRoutingServiceImplTest {

    private final ChannelRoutingServiceImpl routingService = new ChannelRoutingServiceImpl();

    private Notification notification(Severity severity, List<String> requestedChannels) {
        return new Notification(UUID.randomUUID(), UUID.randomUUID().toString(), "test-system", "evt-1",
                "ALERT", severity, Priority.NORMAL, NotificationStatus.RECEIVED, requestedChannels,
                Instant.now(), null, null);
    }

    private Recipient recipientWithPreference(List<Channel> preferences) {
        return new Recipient(UUID.randomUUID(), "user-x", preferences, null);
    }

    @Test
    void statedPreferenceOverridesEvenCriticalSeverity() {
        Notification n = notification(Severity.CRITICAL, List.of("EMAIL", "SMS"));
        Recipient r = recipientWithPreference(List.of(Channel.EMAIL)); // explicit: email only

        List<Channel> resolved = routingService.resolveChannels(n, r);

        // Not forced onto SMS despite CRITICAL — a stated preference is an absolute override.
        assertThat(resolved).containsExactly(Channel.EMAIL);
    }

    @Test
    void noOverlapBetweenRequestedAndPreferredStillDeliversViaRequested() {
        Notification n = notification(Severity.LOW, List.of("PUSH"));
        Recipient r = recipientWithPreference(List.of(Channel.EMAIL)); // preference doesn't include PUSH

        List<Channel> resolved = routingService.resolveChannels(n, r);

        // Falls back to the requested set rather than silently suppressing the notification.
        assertThat(resolved).containsExactly(Channel.PUSH);
    }

    @Test
    void criticalSeverityEscalatesToSmsOnlyWhenNoPreferenceIsStated() {
        Notification n = notification(Severity.CRITICAL, List.of("EMAIL"));
        Recipient r = recipientWithPreference(List.of()); // no stated preference at all

        List<Channel> resolved = routingService.resolveChannels(n, r);

        assertThat(resolved).containsExactlyInAnyOrder(Channel.EMAIL, Channel.SMS);
    }

    @Test
    void nonCriticalSeverityWithNoPreferenceDoesNotEscalate() {
        Notification n = notification(Severity.LOW, List.of("EMAIL"));
        Recipient r = recipientWithPreference(List.of());

        List<Channel> resolved = routingService.resolveChannels(n, r);

        assertThat(resolved).containsExactly(Channel.EMAIL);
    }
}
