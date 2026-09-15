package com.schwab.notification.service.impl;

import com.schwab.notification.domain.Channel;
import com.schwab.notification.domain.Notification;
import com.schwab.notification.domain.Recipient;
import com.schwab.notification.domain.Severity;
import com.schwab.notification.service.ChannelRoutingService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Routing policy (requirement 4.3):
 *
 *   1. Parse the requested channels from the request.
 *   2. If the recipient has stated a preference, it is an absolute
 *      override — severity never adds a channel the recipient didn't
 *      consent to. If the requested channels don't overlap the stated
 *      preference at all, fall back to the requested channels as-is
 *      (a separate, already-documented ambiguous-requirement decision;
 *      it does not fall through to the escalation step below).
 *   3. Only when the recipient has stated no preference at all is there
 *      nothing to override — there, a CRITICAL notification escalates to
 *      also include SMS, on the reasoning that a stronger default is
 *      reasonable when no explicit consent has been withheld.
 *
 * Preference beats severity, not the other way round, because forcing an
 * unrequested, unconsented channel (SMS in particular) onto a recipient
 * who explicitly chose otherwise is a real regulatory concern in a
 * financial context — not just a UX nicety.
 */
@Service
public class ChannelRoutingServiceImpl implements ChannelRoutingService {

    @Override
    public List<Channel> resolveChannels(Notification notification, Recipient recipient) {
        Set<Channel> requested = new LinkedHashSet<>();
        for (String raw : notification.getRequestedChannels()) {
            try {
                requested.add(Channel.valueOf(raw.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // unknown channel name in the request — skipped rather than failing the whole submission
            }
        }

        List<Channel> preferred = recipient.getPreferredChannels();

        if (preferred != null && !preferred.isEmpty()) {
            // Stated preference exists — absolute override; severity does not apply here.
            Set<Channel> intersection = new LinkedHashSet<>(requested);
            intersection.retainAll(preferred);
            return intersection.isEmpty() ? List.copyOf(requested) : List.copyOf(intersection);
        }

        // No stated preference at all — nothing to override, so severity may escalate.
        if (notification.getSeverity() == Severity.CRITICAL) {
            requested.add(Channel.SMS);
        }
        return List.copyOf(requested);
    }
}
