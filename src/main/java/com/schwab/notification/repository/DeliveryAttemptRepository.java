package com.schwab.notification.repository;

import com.schwab.notification.domain.DeliveryAttempt;
import com.schwab.notification.domain.DeliveryAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    List<DeliveryAttempt> findByStatusAndNextAttemptAtBefore(DeliveryAttemptStatus status, Instant cutoff);

    // Used by DeliveryWorker to recompute the parent Notification's status without
    // relying on the lazy Notification.deliveryAttempts collection outside a session.
    List<DeliveryAttempt> findByNotification_Id(UUID notificationId);

    /**
     * Only a row still in fromStatus gets flipped to IN_PROGRESS, so a second
     * poller tick (or a second instance) can't pick up the same row twice.
     * This single atomic UPDATE is what keeps reprocessing a queued delivery
     * from creating uncontrolled duplicate side effects (requirement 4.4).
     * fromStatus is PENDING for a first attempt or RETRY_SCHEDULED for a retry.
     *
     * @Transactional here is required, not decorative: unlike save()/findBy*
     * (which SimpleJpaRepository already runs inside their own transaction),
     * a custom @Modifying query gets no implicit transaction from Spring
     * Data — Hibernate rejects an UPDATE/DELETE JPQL query with "No active
     * transaction" otherwise. Declaring it here (rather than on the caller)
     * keeps this one claim as its own short-lived transaction, which is what
     * lets it commit and become visible to a second poller tick immediately.
     */
    @Transactional
    @Modifying
    @Query("update DeliveryAttempt d set d.status = 'IN_PROGRESS' where d.id = :id and d.status = :fromStatus")
    int claim(@Param("id") UUID id, @Param("fromStatus") DeliveryAttemptStatus fromStatus);
}
