package com.schwab.notification.repository;

import com.schwab.notification.domain.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RecipientRepository extends JpaRepository<Recipient, UUID> {
    Optional<Recipient> findByExternalRef(String externalRef);
}
