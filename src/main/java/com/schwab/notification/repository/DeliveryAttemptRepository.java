package com.schwab.notification.repository;

import com.schwab.notification.domain.DeliveryAttempt;
import com.schwab.notification.domain.DeliveryAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    List<DeliveryAttempt> findByStatusAndNextAttemptAtBefore(DeliveryAttemptStatus status, Instant cutoff);

    /**
     * Only rows still PENDING get flipped to IN_PROGRESS, so a second
     * poller tick (or a second instance) can't pick up the same row twice.
     * This is what keeps reprocessing a queued delivery from creating
     * uncontrolled duplicate side effects (requirement 4.4).
     */
    @Modifying
    @Query("update DeliveryAttempt d set d.status = 'IN_PROGRESS' where d.id = :id and d.status = 'PENDING'")
    int claim(UUID id);
}
