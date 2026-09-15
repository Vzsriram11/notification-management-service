# Scenario 2: Brownfield — Splitting PUSH into iOS (APNs) and Android (FCM)

## Framing

The assignment's own brackets for this scenario were "[a new notification
channel / deduplication / refactor provider-specific logic]." I picked the
third option: treat the already-built `PUSH` channel as the "existing
system" and refactor its provider-specific logic. A single undifferentiated
`PushChannelProvider` can't actually represent real push delivery, since
iOS (APNs) and Android (FCM) have different payload shapes, auth models
(certificates vs. server keys), and failure semantics. That mismatch is
exactly what a brownfield pass is supposed to surface and fix.

## What changed, across layers

- **Domain** — new `DevicePlatform` enum (`IOS`, `ANDROID`); `Recipient`
  gained a `devicePlatform` field.
- **Provider layer** — new `PlatformPushSender` interface with
  `ApnsPushSender` and `FcmPushSender` implementations; `PushChannelProvider`
  itself became a thin dispatcher — a `Map<DevicePlatform, PlatformPushSender>`
  built from a `List<PlatformPushSender>` constructor injection (Spring
  auto-collects every matching bean, the same pattern `ProviderRegistry`'s
  own comment documents at the channel level, applied one level down
  within PUSH).
- **Persistence** — no schema-breaking change: `devicePlatform` is a new
  nullable column on `recipients`, so existing rows are unaffected
  (Hibernate `ddl-auto: update` adds it).
- **Seed/test data** — `RecipientSeeder` updated: `user-1` → iOS, `user-2`
  → Android, `user-3` → no device on file (used to exercise the failure
  branch below).
- **Failure handling** — a recipient with no registered device, or a
  platform with no matching sender, now fails with
  `DeliveryFailureType.INVALID_RECIPIENT` rather than throwing —
  `FailureClassifier` correctly treats that as non-retryable, since
  retrying doesn't fix a missing device registration.

## Why this is a real multi-layer change, not a cosmetic one

Adding a channel, or an enum value, is the "easy" brownfield answer. This
instead forces every layer that touches PUSH to actually engage with
platform identity: the domain model needs to know which platform a
device is on, the provider layer needs a real dispatch decision (not just
a channel lookup), the seed data needs updating to make that dispatch
observable, and the failure path needs a genuinely new failure mode (no
device on file) that didn't exist before.

## Execution

AI-assisted for the mechanical parts — the new enum, the new interface,
two structurally identical sender stubs following the existing
`EmailChannelProvider` pattern, and the dispatcher's map-building
boilerplate. I made the design call (a thin dispatcher plus one sender per
platform, rather than branching inside a single class) and specified the
exact failure-mode behavior (`INVALID_RECIPIENT`, not an exception) before
any code was written.

## Validation

`PushChannelProviderTest` — three tests against the dispatcher directly (no
Spring context, no dependency on the async worker, since `DeliveryWorker`
didn't exist yet when this was built): an iOS recipient routes to the APNs
stub, an Android recipient routes to the FCM stub, and a recipient with no
device platform returns `INVALID_RECIPIENT` rather than throwing.
Re-confirmed end-to-end once `DeliveryWorker` existed, by submitting
notifications to `user-1`/`user-2` (real device platforms) and `user-3`
(no device) and checking `delivery_attempts.status` in the H2 console.
