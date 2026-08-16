package com.simats.burnouttracker.data.models

/**
 * Which spans of a day's device usage belong to which account.
 *
 * WHY A SINGLE START TIMESTAMP IS NOT ENOUGH
 * The previous model recorded one instant per (uid, date) and clamped the daily
 * query's lower bound to it. That silently mis-attributes usage the moment two
 * accounts share a day: a lower bound can move the START of a window, but it
 * cannot punch a hole in the MIDDLE of one. Observed on-device — account A
 * opened its window at 12:14, B signed in at 12:19 and used the phone until
 * 12:23, and A's query (12:14 → now) counted all of B's foreground time as A's,
 * because Android's UsageStats is device-level and records only that the app was
 * in the foreground, never who was signed in.
 *
 * THE UNIT IS AN INTERVAL, NOT AN INSTANT
 * An account's usage for a day is the union of the intervals during which that
 * account was actually signed in:
 *
 *     A  [12:14 → 12:19]  +  [12:23 → now]
 *     B  [12:19 → 12:23]
 *
 * Time when nobody was signed in belongs to nobody and is counted for no
 * account. That is deliberate: attributing it to whoever happened to sign in
 * next is exactly the defect this replaces.
 *
 * This is a query-attribution rule. It records no usage of its own, and it never
 * modifies stored history, burnout assessments or sleep rows.
 */
object UsageOwnership {

    /** Sentinel for an interval that has not been closed yet. */
    const val OPEN = 0L

    /** One span of time during which an account was signed in. [end] may be [OPEN]. */
    data class Interval(val start: Long, val end: Long)

    /** Storage key: per account, per local calendar day. */
    fun key(uid: String, dateKey: String): String = "usage_intervals_${uid}_$dateKey"

    // ── serialisation ────────────────────────────────────────────────────────

    /**
     * `start:end` pairs, comma separated, open interval encoded with end = 0.
     *
     * Plain text rather than JSON so the value stays readable in a prefs dump —
     * the same reasoning that made SleepSession.syncState a string. Malformed
     * input yields an empty list rather than throwing: a corrupt record must
     * degrade to "this account owns nothing today", never to a crash on a
     * usage screen, and never to inheriting someone else's time.
     */
    fun parse(raw: String?): List<Interval> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(',').mapNotNull { piece ->
            val parts = piece.split(':')
            if (parts.size != 2) return@mapNotNull null
            val start = parts[0].toLongOrNull() ?: return@mapNotNull null
            val end = parts[1].toLongOrNull() ?: return@mapNotNull null
            if (start <= 0L) null else Interval(start, end)
        }
    }

    fun encode(intervals: List<Interval>): String =
        intervals.joinToString(",") { "${it.start}:${it.end}" }

    // ── transitions ──────────────────────────────────────────────────────────

    /**
     * This account's session begins at [now].
     *
     * Any interval still open is closed at [now] FIRST. An open interval at this
     * point means the previous session ended without a clean logout — a crash, a
     * force-stop, or the process being killed — and leaving it open would let
     * every later minute accrue to that session forever. Closing it here caps it
     * at the last moment we can actually vouch for.
     */
    fun openSession(existing: List<Interval>, now: Long): List<Interval> =
        closeSession(existing, now) + Interval(now, OPEN)

    /** This account's session ends at [now]. A list with no open interval is unchanged. */
    fun closeSession(existing: List<Interval>, now: Long): List<Interval> =
        existing.map { if (it.end == OPEN) it.copy(end = now) else it }

    /**
     * The intervals as they stand AT [now], with any open one closed at [now].
     *
     * Used for querying without writing: the open interval is still accruing, so
     * a read has to treat it as running up to the present moment.
     */
    fun resolve(existing: List<Interval>, now: Long): List<Interval> =
        closeSession(existing, now).filter { it.end > it.start }

    // ── querying ─────────────────────────────────────────────────────────────

    /**
     * Intersects [intervals] with `[windowStart, windowEnd]`, dropping anything
     * that does not overlap.
     *
     * [windowStart] carries the account-lifetime `data_start` clamp, so both
     * bounds still apply: an account can never see before it existed on this
     * device, nor outside the spans it was signed in for.
     */
    fun clip(intervals: List<Interval>, windowStart: Long, windowEnd: Long): List<Interval> =
        intervals.mapNotNull {
            val start = maxOf(it.start, windowStart)
            val end = minOf(if (it.end == OPEN) windowEnd else it.end, windowEnd)
            if (end > start) Interval(start, end) else null
        }

    /** Total owned time, for diagnostics and tests. Assumes non-overlapping input. */
    fun totalMillis(intervals: List<Interval>): Long =
        intervals.sumOf { (if (it.end == OPEN) 0L else it.end) - it.start }.coerceAtLeast(0L)

    /** Whether an entry is recent enough to keep when pruning old days. */
    fun shouldRetain(dateKey: String, oldestDateKeyToKeep: String): Boolean =
        dateKey >= oldestDateKeyToKeep
}
