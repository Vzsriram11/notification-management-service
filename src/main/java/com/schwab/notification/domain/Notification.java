package com.schwab.notification.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Root aggregate for notification submission (requirement 4.1).
 * idempotencyKey carries a unique constraint — that's the deduplication
 * boundary for requirement 4.4.
 */
@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(name = "uk_idempotency_key", columnNames = "idempotencyKey"))
public class Notification {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private String sourceSystem;

    @Column(nullable = false)
    private String eventId; // correlation id from the source system

    @Column(nullable = false)
    private String notificationType;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @ElementCollection
    private List<String> requestedChannels; // raw request; ChannelRoutingService resolves the actual set

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryAttempt> deliveryAttempts;

    private Instant createdAt;
    private Instant scheduledAt;   // optional, requirement 4.1
    private Instant expiresAt;     // optional, requirement 4.1

    protected Notification() {
        // JPA
    }

    public Notification(UUID id, String idempotencyKey, String sourceSystem, String eventId,
                         String notificationType, Severity severity, Priority priority,
                         NotificationStatus status, List<String> requestedChannels,
                         Instant createdAt, Instant scheduledAt, Instant expiresAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.sourceSystem = sourceSystem;
        this.eventId = eventId;
        this.notificationType = notificationType;
        this.severity = severity;
        this.priority = priority;
        this.status = status;
        this.requestedChannels = requestedChannels;
        this.createdAt = createdAt;
        this.scheduledAt = scheduledAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getSourceSystem() { return sourceSystem; }
    public String getEventId() { return eventId; }
    public String getNotificationType() { return notificationType; }
    public Severity getSeverity() { return severity; }
    public Priority getPriority() { return priority; }
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
    public List<String> getRequestedChannels() { return requestedChannels; }
    public List<DeliveryAttempt> getDeliveryAttempts() { return deliveryAttempts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
