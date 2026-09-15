# Architecture Overview

## Components

| Layer | Package | Responsibility |
|---|---|---|
| API | `api`, `api/dto` | REST surface: `POST /notifications`, `GET /notifications/{id}/status` |
| Domain | `domain` | JPA entities + enums: `Notification`, `DeliveryAttempt`, `Recipient`, `AuditEvent`, `Channel`, `DevicePlatform`, `Severity`, `Priority`, `NotificationStatus`, `DeliveryAttemptStatus` |
| Service | `service`, `service/impl` | Business logic: submission (dedup + fan-out to delivery attempts), channel routing (4.3 policy), status lookup, audit recording |
| Provider | `provider` | One `NotificationChannelProvider` per channel (Email, SMS, Push); `PushChannelProvider` further dispatches to a `PlatformPushSender` per device platform (APNs/FCM) — the brownfield refactor |
| Retry | `retry` | `FailureClassifier` (retryable vs. terminal) + `RetryPolicy` (bounded exponential backoff) |
| Worker | `worker` | `DeliveryWorker` — scheduled outbox poller: claim → dispatch → record outcome → update notification status |
| Repository | `repository` | Spring Data JPA repositories, including the atomic `claim()` UPDATE |
| Config | `config` | `ProviderSimulationProperties`, `AsyncConfig` (`@EnableScheduling`), `RecipientSeeder` (demo data) |

## Tools & execution approach

Built engineer-led with Claude (Cowork) as an accelerator, per the
assignment's own framing: AI assisted *within* tasks — scaffolding
boilerplate, generating structurally repetitive code (e.g. the provider
implementations) from a pattern I established, and drafting these docs from
the actual finished implementation — while I made every architectural and
policy decision (routing precedence, the status model, retry
classification, brownfield scope, what *not* to build for the ambiguous
spec gap) and verified every change myself end-to-end before moving on.
See `docs/ai-usage-log.md` for specifics, including two real defects
AI-generated code introduced that I caught and fixed.

Execution ran as a sequence of scoped sessions rather than one continuous
AI-driven build:

1. Foundations + greenfield (submission, routing, status, idempotency)
2. Async processing (outbox worker, simulated providers, retry/backoff, audit)
3. Brownfield (split PUSH into iOS/Android-specific senders)
4. Ambiguous requirement (spec-gap flag + routing-policy defense, with dedicated tests)
5. Deliverable docs (this set)

## Control flow

```
POST /notifications
  -> NotificationSubmissionService.submit()
       -> idempotency check (unique key) -> short-circuit + audit if duplicate
       -> persist Notification (status=RECEIVED)
       -> for each recipient:
            -> ChannelRoutingService.resolveChannels()  [requirement 4.3 policy]
            -> persist one DeliveryAttempt per resolved channel (status=PENDING)
       -> Notification status -> ROUTED

DeliveryWorker (scheduled poll, default every 2s)
  -> find PENDING / RETRY_SCHEDULED attempts due now
  -> claim()  [atomic UPDATE, its own transaction]  -> IN_PROGRESS
  -> dispatch to the NotificationChannelProvider for the attempt's channel
       (PushChannelProvider -> PlatformPushSender, chosen by DevicePlatform)
  -> outcome success?
       -> SUCCEEDED, audit DELIVERY_SUCCEEDED
     outcome failure?
       -> FailureClassifier.isRetryable(failureType)?
            retryable & attempts remain -> RETRY_SCHEDULED + RetryPolicy.nextDelay(),
                                            audit DELIVERY_RETRY_SCHEDULED
            otherwise                   -> FAILED, audit DELIVERY_FAILED
  -> recompute the parent Notification's status from all its attempts
       (PROCESSING / DELIVERED / PARTIALLY_DELIVERED / FAILED)

GET /notifications/{id}/status -> NotificationStatusService reads current state
```

## Key decisions

- **Outbox + `@Scheduled` poller instead of a real broker.** Kafka/SQS
  would be the production choice for ordering, backpressure, and
  horizontal scaling. A polling table is a documented, time-boxed stand-in
  that still gets the core guarantee right: the atomic claim prevents
  double-processing.
- **A custom two-level status model.** `Notification`:
  `RECEIVED → ROUTED → PROCESSING → DELIVERED/PARTIALLY_DELIVERED/FAILED/EXPIRED/SUPPRESSED`;
  `DeliveryAttempt`: `PENDING/IN_PROGRESS/SUCCEEDED/FAILED/RETRY_SCHEDULED/SUPPRESSED`.
  Requirement 4.2 explicitly allows a documented alternative to whatever
  default state model it implies, and a two-level model is what actually
  answers "what happened, and where" for a notification with multiple
  recipients and channels.
- **Idempotency at two separate boundaries.** A unique constraint on
  `idempotencyKey` blocks a duplicate *notification* from being created
  (requirement 4.4's first bullet). The atomic `claim()` UPDATE blocks a
  duplicate *delivery side effect* if the same attempt is reprocessed
  (4.4's second bullet). These are two separate mechanisms on purpose —
  they guard two separate failure modes.
- **Provider strategy pattern.** `NotificationChannelProvider` means adding
  a channel is a new class, not a new branch in shared dispatch code —
  this is what made the brownfield PUSH/APNs/FCM split low-risk. See
  `docs/scenario-brownfield.md`.
- **Metadata-only audit trail.** `AuditEvent` never stores message bodies,
  contact details, or credentials — only event type, timestamps,
  notification/attempt IDs, and channel — per requirement 4.9's
  constraint.
- **Simulated providers, real failure/retry mechanics.**
  `ProviderSimulationProperties` (`failure-rate`/`timeout-rate`) lets every
  retry branch be exercised on demand instead of hoping a real outage
  occurs during review.
