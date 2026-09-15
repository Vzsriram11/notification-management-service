package com.schwab.notification.worker;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Asynchronous processing for requirement 4.1, using an outbox-poller
 * design: finds PENDING/RETRY_SCHEDULED delivery attempts due now, claims
 * each atomically (DeliveryAttemptRepository.claim), dispatches to the
 * right provider, then records the outcome and an audit event.
 *
 * This stands in for a real broker (Kafka/SQS) — worth naming as the
 * production evolution path in the architecture notes.
 */
@Component
public class DeliveryWorker {

    @Scheduled(fixedDelayString = "${notification.worker.poll-interval-ms:2000}")
    public void pollAndDispatch() {
        // TODO: implement
    }
}
