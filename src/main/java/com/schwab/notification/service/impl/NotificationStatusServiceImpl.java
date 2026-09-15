package com.schwab.notification.service.impl;

import com.schwab.notification.api.dto.DeliveryStatusView;
import com.schwab.notification.api.dto.NotificationStatusResponse;
import com.schwab.notification.domain.DeliveryAttempt;
import com.schwab.notification.domain.Notification;
import com.schwab.notification.repository.NotificationRepository;
import com.schwab.notification.service.NotificationStatusService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationStatusServiceImpl implements NotificationStatusService {

    private final NotificationRepository notificationRepository;

    public NotificationStatusServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public NotificationStatusResponse getStatus(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException("No notification with id " + notificationId));

        List<DeliveryStatusView> deliveries = notification.getDeliveryAttempts().stream()
                .map(this::toView)
                .collect(Collectors.toList());

        List<String> selectedChannels = deliveries.stream()
                .map(d -> d.channel().name())
                .distinct()
                .collect(Collectors.toList());

        return new NotificationStatusResponse(
                notification.getId(),
                notification.getStatus(),
                selectedChannels,
                deliveries,
                notification.getCreatedAt(),
                notification.getCreatedAt() // TODO: a real updatedAt once attempts start mutating status
        );
    }

    private DeliveryStatusView toView(DeliveryAttempt attempt) {
        return new DeliveryStatusView(
                attempt.getRecipient().getExternalRef(),
                attempt.getChannel(),
                attempt.getStatus(),
                attempt.getAttemptCount(),
                attempt.getLastAttemptedAt()
        );
    }
}
