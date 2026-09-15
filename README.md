# Notification Management Service

A prototype Notification Management Service: accepts notification requests
from source systems, resolves delivery channels per recipient, dispatches
asynchronously through simulated Email / SMS / Push (iOS + Android)
providers with bounded retry, and exposes status and audit history.

## Stack

- Java 21, Spring Boot 4.1.1, Maven
- Spring Web MVC, Spring Data JPA, Bean Validation
- H2 (file-mode) — zero-setup, disk-persisted database for this prototype;
  Postgres + Testcontainers is the named production path (see
  `docs/architecture-overview.md`)
- JUnit 5 + AssertJ

## Running it

1. `./mvnw spring-boot:run` (or run `NotificationManagementServiceApplication`
   from your IDE)
2. The app starts on `http://localhost:8080` and seeds three example
   recipients on startup (`RecipientSeeder`):
   - `user-1` — preference `EMAIL`, iOS device on file
   - `user-2` — preference `SMS, PUSH`, Android device on file
   - `user-3` — no stated preference, no device on file
3. H2 console: `http://localhost:8080/h2-console` — JDBC URL
   `jdbc:h2:file:./data/notifications;AUTO_SERVER=TRUE`, user `sa`, empty
   password.

## Try it

`./test-api.sh` submits one notification and fetches its status. Or
directly:

```bash
curl -X POST http://localhost:8080/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "demo-001",
    "sourceSystem": "trading-platform",
    "eventId": "evt-001",
    "notificationType": "ALERT",
    "severity": "CRITICAL",
    "priority": "URGENT",
    "recipientRefs": ["user-1"],
    "requestedChannels": ["EMAIL", "SMS"]
  }'
```

```bash
curl http://localhost:8080/notifications/{notificationId}/status
```

Resubmitting the same `idempotencyKey` returns `"deduplicated": true`
instead of creating a second notification.

## Exercising retry / failure handling

Set `notification.simulation.failure-rate` and/or
`notification.simulation.timeout-rate` (0.0–1.0) in
`src/main/resources/application.yml`, restart, and submit a notification —
the H2 console's `delivery_attempts` and `audit_events` tables will show
attempts cycling through `RETRY_SCHEDULED` with exponential backoff before
landing on `SUCCEEDED` or `FAILED`. Reset both to `0.0` afterward.

## Project layout

```
domain/          entities & enums — Notification, DeliveryAttempt, Recipient,
                 AuditEvent, Channel, DevicePlatform, Severity, Priority,
                 NotificationStatus, DeliveryAttemptStatus
api/, api/dto/   REST controller + request/response DTOs
service/,
service/impl/    submission, channel routing, status lookup, audit
provider/        one NotificationChannelProvider per channel (Email, SMS,
                 Push); Push further splits into APNs/FCM PlatformPushSender
retry/           failure classification + bounded exponential backoff
worker/          the outbox poller (DeliveryWorker)
repository/      Spring Data JPA repositories
config/          simulation properties, async/scheduling config, recipient
                 seeder
```

## Documentation

- `docs/architecture-overview.md` — components, control flow, key decisions
- `docs/scenario-greenfield.md`, `docs/scenario-brownfield.md`,
  `docs/scenario-ambiguous.md` — the three required scenarios
- `docs/testing-and-limitations.md` — testing approach, known limitations,
  trade-offs
- `docs/ai-usage-log.md` — how AI assistance was used, and where engineering
  judgment and ownership sat
