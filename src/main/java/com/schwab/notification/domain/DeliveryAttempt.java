package com.schwab.notification.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * One (recipient, channel) delivery attempt. This is the row the outbox
 * worker claims atomically, so reprocessing a queued delivery can't create
 * uncontrolled duplicate side effects (requirement 4.4).
 */
@Entity
@Table(name = "delivery_attempts")
public class DeliveryAttempt {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    private Notification notification;

    @ManyToOne(optional = false)
    private Recipient recipient;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    private DeliveryAttemptStatus status;

    private int attemptCount;
    private Instant nextAttemptAt; // backoff schedule, requirement 4.5

    @Version
    private long version; // optimistic lock, alongside the atomic claim update

    private Instant createdAt;
    private Instant lastAttemptedAt;

    protected DeliveryAttempt() {
        // JPA
    }

    public DeliveryAttempt(UUID id, Notification notification, Recipient recipient, Channel channel,
                            DeliveryAttemptStatus status, int attemptCount,
                            Instant createdAt, Instant lastAttemptedAt, Instant nextAttemptAt) {
        this.id = id;
        this.notification = notification;
        this.recipient = recipient;
        this.channel = channel;
        this.status = status;
        this.attemptCount = attemptCount;
        this.createdAt = createdAt;
        this.lastAttemptedAt = lastAttemptedAt;
        this.nextAttemptAt = nextAttemptAt;
    }

    public UUID getId() { return id; }
    public Notification getNotification() { return notification; }
    public Recipient getRecipient() { return recipient; }
    public Channel getChannel() { return channel; }
    public DeliveryAttemptStatus getStatus() { return status; }
    public void setStatus(DeliveryAttemptStatus status) { this.status = status; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastAttemptedAt() { return lastAttemptedAt; }
    public void setLastAttemptedAt(Instant lastAttemptedAt) { this.lastAttemptedAt = lastAttemptedAt; }
}
