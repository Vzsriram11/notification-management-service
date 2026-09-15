# Testing Approach, Limitations, and Trade-offs

## Testing approach

- **Unit tests** for logic that doesn't need a Spring context: JUnit 5 +
  AssertJ.
  - `ChannelRoutingServiceImplTest` — all four branches of the routing
    precedence policy (requirement 4.3).
  - `BoundedExponentialBackoffRetryPolicyTest` — backoff schedule and its
    cap.
  - `DefaultFailureClassifierTest` — retryable vs. terminal failure types.
  - `PushChannelProviderTest` — platform dispatch (iOS/Android/no-device).
- **Manual end-to-end verification** — `test-api.sh` plus ad-hoc `curl`
  against a running instance: successful submission, idempotent
  resubmission (`"deduplicated": true`, no second row created), and status
  retrieval.
- **Retry/failure-path verification** —
  `notification.simulation.failure-rate` / `timeout-rate` make every retry
  branch reproducible on demand rather than waiting for a real outage;
  verified by watching `delivery_attempts` cycle through
  `RETRY_SCHEDULED` with the expected backoff and `audit_events` record
  each transition.
- **Database inspection** — the H2 console was used throughout to confirm
  actual persisted state (row counts, status values, the `channel` column
  on audit events) rather than trusting API responses alone.
- **A full recorded trace** — `docs/assessment-testing-screenshots.pdf`
  walks one notification through the whole system: `POST /notifications`
  → status `PENDING` → status `DELIVERED` moments later once the worker
  picked it up, a resubmission with the same idempotency key returning
  `deduplicated: true` with the notification's current status (not a stale
  snapshot), and the resulting `notifications`, `delivery_attempts`, and
  `audit_events` rows in the H2 console for that same notification ID —
  including the six audit events in correct chronological order
  (`NOTIFICATION_ACCEPTED → DELIVERY_QUEUED → ROUTING_DECIDED →
  DELIVERY_SUCCEEDED → NOTIFICATION_STATUS_CHANGED →
  NOTIFICATION_DEDUPLICATED`) and confirmation that the channel-routing
  policy correctly dropped `EMAIL` (outside the recipient's stated
  preference) and kept only `SMS`.

## Known limitations (explicitly out of scope, time-boxed)

- **No real message broker.** The outbox + `@Scheduled` poller stands in
  for Kafka/SQS. It gets the core correctness property right (the atomic
  claim prevents double-processing) but not ordering guarantees,
  backpressure, or horizontal scale-out — a second instance would need
  leader election or partitioned claiming to run safely at real scale.
- **No real provider integrations.** Email/SMS/APNs/FCM are all simulated,
  with configurable failure/timeout rates; there's no actual SMTP,
  Twilio, APNs, or FCM SDK/credential wiring.
- **H2 file-mode database, not Postgres** — chosen for zero-setup
  runnability in a take-home review context. Postgres + Testcontainers is
  the named production/CI path.
- **No recipient-management API.** Recipients and their
  preferences/device platforms are seeded on startup (`RecipientSeeder`);
  a real system would need CRUD for this.
- **Sections 4.6–4.8 of the assignment spec are undocumented** in the
  version I received (see `docs/scenario-ambiguous.md`) — nothing is
  built against them, by design, rather than guessed.
- **No authentication/authorization** on the API — out of scope for a
  prototype, but a real deployment would need it before accepting
  notification submissions from external systems.
- **Single-node scheduling assumption.** `@Scheduled` polling assumes one
  worker instance; the atomic `claim()` prevents double-processing even
  with multiple instances polling concurrently, but there's no leader
  election, so idle-poll overhead scales with instance count.

## Trade-offs made under time constraints

- Retry backoff values (2s/4s/8s/16s/32s, 5 attempts) are hardcoded rather
  than externalized to `application.yml` — reasonable as a demonstrated
  policy, but a production version should make these configurable per
  channel or per notification type.
- All channels currently share one `ProviderSimulationProperties`
  failure/timeout rate rather than per-channel rates — sufficient to
  exercise the retry logic, but not representative of real providers
  having independently different reliability.
- No pagination or filtering on any endpoint — not required by the spec,
  but would matter at scale for an audit-history or bulk-status endpoint.
- **`NotificationStatusServiceImpl` relies on Open-Session-In-View.** It
  reads `notification.getDeliveryAttempts()` — a lazy `@OneToMany` — with
  no explicit transaction. That only works because Spring Boot's
  `spring.jpa.open-in-view` default (`true`) keeps a Hibernate session
  open for the whole HTTP request. `DeliveryWorker` deliberately does
  *not* rely on the same mechanism, since it runs on a background
  scheduling thread with no request-bound session — it queries delivery
  attempts explicitly through the repository instead. OSIV is generally
  discouraged for production systems (it holds a DB connection for the
  full request/response cycle and can mask N+1 queries at render time);
  the honest fix would be to make the status read explicit too, the same
  way the worker already is.
- **`DeliveryAttempt.notification`/`.recipient` are `FetchType.EAGER`,
  not `LAZY`.** This is deliberate, not an oversight: `DeliveryWorker`
  reads both associations from a detached entity on that same background
  thread, where a `LAZY` proxy would throw `LazyInitializationException`
  the instant a provider called `attempt.getRecipient()`. EAGER trades a
  small per-row query cost (fine at this scale) for correctness in that
  path. At a scale where the query cost mattered, the fix would be an
  explicit `JOIN FETCH` query sized to the worker's actual access
  pattern, not a blanket `EAGER` default on the entity.
