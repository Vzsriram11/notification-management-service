# AI Usage Log

Per the assignment's own framing — "AI assists the engineer within tasks;
the engineer owns execution and quality" — this is a plain account of
where AI (Claude, via Cowork) was used, and where I made the calls myself.

## Where AI assisted

- **Scaffolding** — initial entity/enum/repository/DTO boilerplate,
  generated against a design (fields, relationships, package layout) I
  specified up front, not invented by the AI.
- **Repetitive structural code** — e.g. the four `NotificationChannelProvider`
  implementations and two `PlatformPushSender` implementations follow one
  identical pattern; generated once the pattern was established, not
  independently designed four separate times.
- **Drafting these deliverable documents**, from the actual finished code
  and my own decisions — not from a generic template.
- **A sounding board for trade-offs I already had a candidate answer for**
  — e.g. I proposed the channel-routing precedence options myself; AI
  helped stress-test the SMS-consent reasoning before I committed to it.

## Where I owned the decision and the execution

- **All architectural decisions** — the outbox+poller pattern, the
  two-tier status model, the provider-strategy pattern, the atomic-claim
  idempotency mechanism, and what to build (and explicitly not build) for
  the brownfield and ambiguous scenarios.
- **The channel-routing precedence policy itself** (see
  `docs/scenario-ambiguous.md`) — I chose the "preference overrides,
  severity escalates only in a full vacuum" policy and the SMS-consent
  reasoning behind it. That's a judgment call about compliance and user
  trust, not something to delegate.
- **Flagging the spec gap (sections 4.6–4.8) instead of guessing** — a
  decision about how to handle genuine ambiguity, made by me.
- **Every line of code was compiled, run, and manually verified by me**
  before moving forward — via an IntelliJ rebuild, `test-api.sh`, and
  direct H2 console inspection. Nothing here shipped on the strength of
  "it looks right."

## Defects AI-generated code introduced that I caught and fixed

Being concrete about this, since it's the clearest evidence of engineer
ownership rather than autonomous trust:

1. **Record accessor collision** — `DeliveryOutcome.success()`, a static
   factory method, collided with the implicit `boolean success()` accessor
   Java generates for the record's `success` component, causing a compile
   error. Fixed by renaming the factory to `ok()`.
2. **Missing transaction on a `@Modifying` query** — the initial
   `DeliveryAttemptRepository.claim()` ran outside a transaction, throwing
   `InvalidDataAccessApiUsageException: No active transaction for update
   or delete query` the first time the scheduled worker actually ran in
   practice. Root cause: `SimpleJpaRepository`'s implicit per-call
   transaction covers `save`/`findBy*`, but a custom `@Modifying @Query`
   method needs its own explicit `@Transactional`. Fixed by annotating
   `claim()` directly, with a comment recording why — a reusable lesson
   for `@Modifying` queries generally, not just this one.

## What I did not delegate

I did not ask AI to decide the routing precedence, the retry/backoff
numbers, what counts as retryable vs. terminal, the brownfield scope, or
how to handle the spec gap — those are the decisions this assessment is
actually evaluating, and they're mine.
