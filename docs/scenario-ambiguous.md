# Scenario 3: Ambiguous Requirement

The assignment (section 3.3) asks for both "well-defined and ambiguous
requirements" to be handled. Two genuinely different kinds of ambiguity
showed up while building this service, and they call for different
responses — one is flagged and left alone, the other is a judgment call
that's implemented, tested, and defended below.

## Ambiguity 1: The specification has a gap — sections 4.6 through 4.8 are missing

Extracting the assignment PDF with `pdftotext -layout` shows the functional
requirements jump straight from **4.5 Retry and Failure Handling** to
**4.9 Audit History**, with no 4.6, 4.7, or 4.8 anywhere in the document:

```
4.5 Retry and Failure Handling
Implement a bounded retry strategy for retryable delivery failures. ...
[page break]
4.9 Audit History
Record significant actions such as ...
```

This isn't a copy-paste artifact: the text immediately before 4.5 ends and
immediately after 4.9 begins is complete and clean, and the same gap is
visible opening the PDF directly, not just through extraction.

**Decision: flag it, don't guess it.** Inventing three sections of
requirements to fill the numbering gap would mean building against my own
assumption of what Schwab intended — with real risk of contradicting
whatever 4.6–4.8 actually says, or scope-creeping the prototype past what
was asked. Given where the gap sits (between retry/failure handling and
audit history), plausible candidates are things like provider/channel
configuration management, throttling or rate-limit policy, or notification
templating — but none of that is implemented here. Recognizing a genuine
spec gap and stopping rather than fabricating scope is itself the
engineering judgment this scenario is testing.

**Action for the reviewer:** if 4.6–4.8 exist in a version of the document
I didn't receive, please share them — the architecture here (provider
interface, routing service, outbox worker, audit service) is intentionally
modular, so most plausible additions are one new component away rather
than a rearchitecture.

## Ambiguity 2: Channel-routing precedence (requirement 4.3) — resolved and implemented

Requirement 4.3 says channel selection should factor in "Requested
channel / Notification severity / Recipient preferences, Routing policy" —
but doesn't say what wins when they conflict. Concretely: a notification
requests `EMAIL, SMS`, its severity is `CRITICAL`, but the recipient's
stated preference is `EMAIL` only. Does severity force SMS delivery anyway,
overriding what the recipient said they wanted?

### Options considered

| Option | Rule | Problem |
|---|---|---|
| 1 | Severity always wins — CRITICAL forces every applicable channel | Overrides a recipient's stated preference; for SMS specifically this risks sending an unconsented text |
| 2 | Preference always wins, severity never escalates | A recipient who never stated a preference gets no extra reach even for a CRITICAL alert |
| **3 (chosen)** | Preference is an absolute override when one is on file; severity may escalate (add SMS) only when there's no stated preference at all | — |

### Why Option 3

SMS is not equivalent to email or push from a compliance standpoint —
sending a text generally requires the recipient's affirmative opt-in
(TCPA-adjacent consent rules), unlike an email or an app push. Letting
severity silently override a recipient's stated channel preference means
the system could text someone who explicitly said "email me, not SMS,"
which is both a compliance exposure and a trust problem. Escalating to SMS
only when the recipient has *no* stated preference at all sidesteps that
entirely: it's filling a gap, not overriding a choice.

A second, smaller judgment call lives in the same method: if the requested
channels and the recipient's preferred channels don't overlap at all, the
system still delivers via the originally requested channels rather than
suppressing the notification outright. Silently dropping a notification
because of a routing mismatch seemed like a worse failure mode than
delivering it somewhere the recipient didn't explicitly rule out.

### Implementation

`ChannelRoutingServiceImpl.resolveChannels(Notification, Recipient)`:

1. Parse the notification's requested channels.
2. If the recipient has a non-empty stated preference: intersect it with
   the requested channels. If the intersection is non-empty, that's the
   result. If the intersection is empty (no overlap at all), fall back to
   the requested channels as-is.
3. If the recipient has **no** stated preference and the notification's
   severity is `CRITICAL`, add `SMS` to the requested channels.
4. Otherwise, return the requested channels unchanged.

### Test data and verification

`RecipientSeeder` seeds three recipients that exercise each branch:

- **user-1** — preference `[EMAIL]`. A CRITICAL notification requesting
  `EMAIL, SMS` still resolves to `EMAIL` only — preference wins even under
  CRITICAL severity.
- **user-2** — preference `[SMS, PUSH]`. Used to exercise the
  intersection/fallback branch.
- **user-3** — no stated preference. A CRITICAL notification escalates to
  add `SMS`; a non-critical one does not.

`ChannelRoutingServiceImplTest` (`src/test/java/.../service/impl/`) covers
all four branches directly against the routing service, with no Spring
context required:

- `statedPreferenceOverridesEvenCriticalSeverity`
- `noOverlapBetweenRequestedAndPreferredStillDeliversViaRequested`
- `criticalSeverityEscalatesToSmsOnlyWhenNoPreferenceIsStated`
- `nonCriticalSeverityWithNoPreferenceDoesNotEscalate`

This was also verified end-to-end via `test-api.sh` against the seeded
recipients and by inspecting the `audit_events` table (channel column) in
the H2 console, confirming the resolved channel set matches the policy for
real submitted notifications, not just in unit isolation.
