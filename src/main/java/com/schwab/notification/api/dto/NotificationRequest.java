package com.schwab.notification.api.dto;

import com.schwab.notification.domain.Priority;
import com.schwab.notification.domain.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

/** Inbound shape for POST /notifications (requirement 4.1). */
public record NotificationRequest(
        @NotBlank String idempotencyKey,
        @NotBlank String sourceSystem,
        @NotBlank String eventId,
        @NotBlank String notificationType,
        Severity severity,
        Priority priority,
        @NotEmpty List<String> recipientRefs,
        @NotEmpty List<String> requestedChannels,
        Instant scheduledAt,
        Instant expiresAt
) {}
