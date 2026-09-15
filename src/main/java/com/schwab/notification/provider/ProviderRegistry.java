package com.schwab.notification.provider;

import com.schwab.notification.domain.Channel;

/**
 * Not wired up yet, on purpose. Early on it's fine — expected, even — to
 * dispatch on a plain switch/if-else over Channel inside the worker. This
 * registry is what replaces that switch once a new channel is
 * added and the switch statement stops being the cheapest option.
 */
public interface ProviderRegistry {
    NotificationChannelProvider providerFor(Channel channel);
}
