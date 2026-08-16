# App-usage clamp — on-device verification protocol

Release-blocking check for the event-based rewrite of
`UsageStatsHelper.fetchDailyUsage()`.

## What changed and why this needs verifying

`fetchDailyUsage()` previously used `queryAndAggregateUsageStats(start, end)`.
That API is bucket-based: it returns the daily buckets *overlapping* the range,
each carrying its whole-bucket `totalTimeInForeground`. Moving `start` forward
does not shrink those totals — sub-day granularity is not obtainable from it.
The account-isolation clamp was therefore passed in and **silently discarded**,
and a newly adopted account was shown the entire device's daily usage as its own.

It now pairs `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED` events from
`queryEvents()`, so the window bound is genuinely enforced.

Because the old failure was silent, "it looks reasonable" is not acceptable
evidence. The numbers below are predicted in advance so the check can fail.

## The invariant

> No foreground interval that ends before `data_start` may contribute any time,
> to any surface, for the account adopted at `data_start`.

Not "the number should be small" — a specific set of apps must be **absent**.

## Baseline (measured 2026-08-15 23:16 IST, device RZCXC0JFK2V)

Derived by replaying the device's own `dumpsys usagestats` event log through a
port of `UsageEventAccounting.foregroundMillisByPackage`.

- account: padmanabhancba (`2wZjgkBr4bhIyMwm7ifSPiAXIsN2`)
- `data_start_2wZjg…` = `1786814294000` → 22:48:14 IST
- events replayed: 1652 RESUMED/PAUSED

| Surface | Unclamped (old behaviour) | Clamped (expected new) |
|---|---:|---:|
| Total usage | 4.07 h | **0.19 h** |
| Switch count | 807 | **66** |
| Apps counted | 9 | **3** |

Post-clamp apps — the only ones permitted in Top Used Apps:

| App | Post-clamp |
|---|---:|
| `com.whatsapp` | 0.07 h |
| `com.simats.burnouttracker` | 0.06 h |
| `com.openai.chatgpt` | 0.06 h |

Pre-clamp apps — **must not appear at all**:

| App | Pre-clamp time it must not contribute |
|---|---:|
| `com.ng.mangazone` | 1.45 h |
| `com.activision.callofduty.shooter` | 1.45 h |
| `com.brave.browser` | 0.12 h |
| `com.aratai.chat` | 0.12 h |

## Checks

1. **Headline total** — must reflect only post-`data_start` activity. Do not
   require literally `0`: interacting with the device during verification is
   itself genuine post-clamp usage and legitimately raises it. The failure
   signal is a number in the *hours*, near the unclamped 4.07 h.
2. **Top Used Apps** — the sharpest check. `com.ng.mangazone` and
   `com.activision.callofduty.shooter` are the two largest pre-clamp entries at
   1.45 h each. If either is listed, the clamp is not being honoured. This is a
   presence/absence test, so it cannot be explained away by drift.
3. **Category totals** — Social / Gaming / Streaming / Productivity must sum to
   the headline total. They now derive from the same bounded event map, so a
   mismatch means a surface is reading from somewhere else. Gaming in
   particular should be ~0, since the only gaming app is entirely pre-clamp.
4. **Switch count** — order tens, not ~807. It is counted from the same event
   list as the totals, so the two can no longer disagree about the window.
5. **Sleep history** — unchanged at 7 nights owned by yninja004, 0 for
   padmanabhancba. Confirms the restore path did not repopulate.

## Notes on the baseline figures

- 4.07 h is higher than the ~3.2 h seen earlier the same evening simply because
  more device usage accumulated between the two observations.
- The replay omits the `FLAG_SYSTEM` exclusion in
  `UsageStatsHelper.isSystemPackage` (it needs `PackageManager`), so it may
  under-exclude system apps — `com.wssyncmldm` in the unclamped list is one.
  Real in-app totals should therefore be **at or slightly below** these.
- Figures are specific to this device, account and date. Re-derive after any
  change to `data_start` by re-running the replay against a fresh
  `dumpsys usagestats`.

## Prerequisite

Do not install the build carrying `SleepHistoryRestore` until padmanabhancba's
Firestore records have been checked and any contaminated Aug 09–15 entries
removed. Restore trusts the server; installing first would let bad cloud
records repopulate the device and make the repair appear to have failed.

## Automated coverage

`UsageEventAccountingTest` (9 tests, `:app:testDebugUnitTest`) covers the
clamping arithmetic on the JVM. It was mutation-verified: reintroducing
bucket-style behaviour (`to - from`, ignoring window bounds) fails exactly
`usage before the clamp does not count toward a newly adopted account` and
`a session straddling the clamp is counted only from the clamp onward`.
Keep it release-blocking.
