package com.schwab.notification.domain;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

/**
 * A notification recipient plus channel preferences — the routing input
 * for requirement 4.3. Kept as its own entity rather than embedded, since
 * preferences are looked up independently of any single notification.
 */
@Entity
@Table(name = "recipients")
public class Recipient {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String externalRef; // e.g. employee id / customer id from the source system

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<Channel> preferredChannels; // ordered by preference; empty = no stated preference

    protected Recipient() {
        // JPA
    }

    public Recipient(UUID id, String externalRef, List<Channel> preferredChannels) {
        this.id = id;
        this.externalRef = externalRef;
        this.preferredChannels = preferredChannels;
    }

    public UUID getId() { return id; }
    public String getExternalRef() { return externalRef; }
    public List<Channel> getPreferredChannels() { return preferredChannels; }
}
