package com.schwab.notification.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit trail (requirement 4.9). Metadata only, by design —
 * never store message bodies, recipient contact details, or provider
 * credentials here.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    private UUID notificationId;
    private UUID deliveryAttemptId; // nullable — set only for attempt-level events

    @Enumerated(EnumType.STRING)
    private Channel channel; // nullable — set only for attempt-level (per-channel) events

    private String eventType;
    private String detail;
    private Instant occurredAt;

    protected AuditEvent() {
        // JPA
    }

    public AuditEvent(UUID id, UUID notificationId, UUID deliveryAttemptId, Channel channel,
                       String eventType, String detail, Instant occurredAt) {
        this.id = id;
        this.notificationId = notificationId;
        this.deliveryAttemptId = deliveryAttemptId;
        this.channel = channel;
        this.eventType = eventType;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getNotificationId() { return notificationId; }
    public UUID getDeliveryAttemptId() { return deliveryAttemptId; }
    public Channel getChannel() { return channel; }
    public String getEventType() { return eventType; }
    public String getDetail() { return detail; }
    public Instant getOccurredAt() { return occurredAt; }
}
