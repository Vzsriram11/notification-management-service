package com.schwab.notification.service.impl;

import com.schwab.notification.domain.AuditEvent;
import com.schwab.notification.domain.Channel;
import com.schwab.notification.repository.AuditEventRepository;
import com.schwab.notification.service.AuditService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditServiceImpl(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Override
    public void record(UUID notificationId, UUID deliveryAttemptId, String eventType, String detail) {
        record(notificationId, deliveryAttemptId, null, eventType, detail);
    }

    @Override
    public void record(UUID notificationId, UUID deliveryAttemptId, Channel channel, String eventType, String detail) {
        auditEventRepository.save(new AuditEvent(UUID.randomUUID(), notificationId, deliveryAttemptId,
                channel, eventType, detail, Instant.now()));
    }
}
