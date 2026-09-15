package com.schwab.notification.config;

import com.schwab.notification.domain.Channel;
import com.schwab.notification.domain.DevicePlatform;
import com.schwab.notification.domain.Recipient;
import com.schwab.notification.repository.RecipientRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Seeds a few example recipients with different preference profiles so
 * channel routing (requirement 4.3) has real data to act on.
 */
@Component
public class RecipientSeeder implements CommandLineRunner {

    private final RecipientRepository recipientRepository;

    public RecipientSeeder(RecipientRepository recipientRepository) {
        this.recipientRepository = recipientRepository;
    }

    @Override
    public void run(String... args) {
        seed("user-1", List.of(Channel.EMAIL), DevicePlatform.IOS);                  // narrow, explicit preference; iOS device on file
        seed("user-2", List.of(Channel.SMS, Channel.PUSH), DevicePlatform.ANDROID);  // broader, explicit preference; Android device on file
        seed("user-3", List.of(), null);                                             // no stated preference, no device on file
    }

    private void seed(String externalRef, List<Channel> preferences, DevicePlatform platform) {
        recipientRepository.findByExternalRef(externalRef)
                .orElseGet(() -> recipientRepository.save(
                        new Recipient(UUID.randomUUID(), externalRef, preferences, platform)));
    }
}
