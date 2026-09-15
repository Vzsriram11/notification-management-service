package com.schwab.notification.service;

import com.schwab.notification.domain.Channel;
import com.schwab.notification.domain.Notification;
import com.schwab.notification.domain.Recipient;
import java.util.List;

/**
 * Resolves the actual delivery channel(s) for one recipient (requirement
 * 4.3): requested channel, then severity escalation rules, then recipient
 * preference, then a policy default. The "requested channel but no
 * matching recipient preference" branch is an ambiguous requirement,
 * resolved here with an explicit, documented fallback rather than a guess.
 */
public interface ChannelRoutingService {
    List<Channel> resolveChannels(Notification notification, Recipient recipient);
}
