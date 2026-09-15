# Scenario 1: Greenfield

## What was built

The initial notification-management capability, end to end: submission,
recipient/channel resolution, asynchronous delivery, and status retrieval
(requirement 3.1 / 4.1–4.4).

## Decomposition

1. **Domain model** — `Notification`, `Recipient`, `DeliveryAttempt`,
   `AuditEvent` entities; `Channel`/`Severity`/`Priority`/
   `NotificationStatus`/`DeliveryAttemptStatus` enums.
2. **Submission API** — `POST /notifications`, a validated request DTO, and
   an idempotency check against a unique key.
3. **Channel routing** — resolves the actual delivery channel set per
   recipient. (See `docs/scenario-ambiguous.md` for the precedence policy
   itself — the one genuinely ambiguous piece of an otherwise well-defined
   requirement.)
4. **Persistence of delivery attempts** — one `DeliveryAttempt` per
   (recipient, resolved channel) pair.
5. **Status API** — `GET /notifications/{id}/status`.
6. **Audit trail** — every meaningful transition recorded as an
   `AuditEvent`.

## Execution

Built as a straight-line vertical slice — each requirement wired fully
end-to-end (entity → repository → service → controller) before moving to
the next, rather than all entities, then all services, then all
controllers. That kept every session endable in a runnable state instead
of mid-scaffold. AI-assisted for the initial entity/repository/DTO/
controller boilerplate against a design I specified up front; I wrote and
iterated the actual submission and routing logic myself, and fixed the
compile/runtime defects AI-generated code introduced along the way (see
`docs/ai-usage-log.md`).

## Validation

- `test-api.sh` — submits a notification, fetches its status, and confirms
  both the response shape and the persisted state.
- Manual duplicate-submission test — resubmitting the same
  `idempotencyKey` returns `"deduplicated": true` and does not create a
  second `Notification` row (confirmed via H2 console row count).
- H2 console inspection — confirms `DeliveryAttempt` rows are created per
  resolved channel per recipient, and `AuditEvent` rows exist for
  `NOTIFICATION_ACCEPTED`, `ROUTING_DECIDED`, and `DELIVERY_QUEUED`.

At this stage (before the async-processing work), delivery attempts sit at
`PENDING` — actually *processing* them asynchronously is substantial
enough that it's covered in its own part of the build (still within
requirement 4.1's "asynchronous processing" scope); see
`docs/architecture-overview.md`'s control-flow diagram for how submission
and the delivery worker connect.
