package com.simats.burnouttracker.utils

import com.simats.burnouttracker.data.models.RemoteSleepLog
import kotlin.math.roundToInt

/**
 * Decides what a cloud restore is allowed to write, for one account.
 *
 * Split out of [SleepHistoryRestore] so the ownership rules can be tested on the
 * JVM without a device, Room or a network. The rules are the whole point of this
 * class, so they must be executable in a test rather than asserted in a comment.
 *
 * THE DEDUPLICATION KEY IS `(ownerUid, date)` — NEVER THE SLEEP VALUES.
 *
 * That distinction is the entire safety property. Two accounts on one device can
 * legitimately hold nights with byte-identical sleepStart/sleepEnd — the phone
 * derives both from the same device-level UsageStats — so treating equal values
 * as proof of shared ownership would silently merge two people's history. Value
 * equality is never consulted here.
 *
 * What restore may do:
 *  - insert a row owned by the restoring account, for a date it does not have;
 *
 * What restore may never do:
 *  - modify, delete or re-own a row belonging to any other account;
 *  - modify a row the restoring account already has, merely because the server
 *    also holds that night;
 *  - infer ownership from matching sleep values.
 *
 * Another account already holding the same date is NOT a reason to skip: that
 * account's row stays exactly as it is, and the restoring account still gets its
 * own row. Both facts are recorded so the decision is visible in the log.
 */
internal object SleepRestorePlanner {

    /** The `(date, ownerUid)` of one row already in Room. Values deliberately absent. */
    data class LocalRow(val date: String, val ownerUid: String)

    /**
     * One of the RESTORING account's own rows, with the fields worth comparing.
     *
     * Only ever built from rows that account already owns, so comparing against
     * it cannot leak another account's values into a decision.
     */
    data class OwnRow(
        val date: String,
        val sleepStart: Long,
        val sleepEnd: Long,
        val totalSleepMinutes: Int,
        /**
         * Room's primary key, carried so a reported conflict names the LOCAL row
         * as precisely as it names the remote document.
         *
         * The Aug-10 investigation cost several rounds because the report
         * identified the cloud record only by (account, date) — which does not
         * locate a legacy document — and identified the local row not at all.
         * Both sides are now addressable directly.
         */
        val localId: Long = 0,
        /** The local row's sync state, so a conflict says whether it ever reached the cloud. */
        val syncState: String = "",
        /** The local row's last sync error, if any. */
        val lastSyncError: String? = null
    )

    /**
     * A row restore intends to insert. Plain data — no Room or Android types — so
     * the planner stays JVM-testable; [SleepHistoryRestore] maps it to the entity.
     */
    data class PlannedRow(
        val date: String,
        val ownerUid: String,
        val sleepStart: Long,
        val sleepEnd: Long,
        val totalSleepMinutes: Int,
        val awakeningCount: Int,
        val sleepQuality: Int,
        val disturbanceScore: Int,
        /**
         * The backend document id this row came from, carried for diagnostics only.
         *
         * Never written to Room and never used in any decision — it exists so a
         * reported [Decision.Conflict] names the exact server record involved.
         * Legacy records predate deterministic ids, so a conflicting document
         * cannot be located from its (user, date) alone.
         */
        val remoteId: String? = null
    )

    sealed interface Decision {
        /** Not a usable detected night (manual entry, missing or nonsensical bounds). */
        data class Unusable(val date: String?, val reason: String) : Decision

        /** The restoring account already holds this date. Nothing is written or altered. */
        data class AlreadyOwned(val date: String) : Decision

        /**
         * The restoring account holds this date but with DIFFERENT values.
         *
         * Reported, never resolved. The local row is what the phone derived from
         * its own UsageStats and may carry wake events the server never stored,
         * so overwriting it with a lossy round-trip would destroy detail; and
         * silently keeping the local one would hide a genuine divergence between
         * device and cloud. Both are decisions for a human, so restore does
         * neither and says so.
         */
        data class Conflict(
            val date: String,
            val local: OwnRow,
            val remote: PlannedRow
        ) : Decision

        /**
         * Insert [row] for the restoring account. [otherOwners] are accounts that
         * already hold this date; they are reported for logging only and their rows
         * are left untouched.
         */
        data class Insert(val row: PlannedRow, val otherOwners: List<String>) : Decision
    }

    /**
     * Plans restore for [uid] given the server's [remote] records and every local
     * row for the relevant dates ([local], across ALL owners).
     *
     * Idempotent in two senses, both required:
     *  - re-running after the plan has been applied yields only [Decision.AlreadyOwned],
     *    because the applied rows are then present in [local];
     *  - within a single pass, two remote records for the same date produce exactly
     *    one insert — the server should not serve duplicates for one automatic
     *    night, but a plan that trusted it to never do so would be one upsert bug
     *    away from writing them.
     */
    fun plan(
        uid: String,
        remote: List<RemoteSleepLog>,
        local: List<LocalRow>,
        ownRows: List<OwnRow> = emptyList()
    ): List<Decision> {
        if (uid.isBlank()) return emptyList()

        val ownedByUid = local.filter { it.ownerUid == uid }.map { it.date }.toSet()
        val ownersByDate = local.filter { it.ownerUid != uid }
            .groupBy({ it.date }, { it.ownerUid })
        val ownByDate = ownRows.associateBy { it.date }

        val plannedDates = mutableSetOf<String>()
        val decisions = mutableListOf<Decision>()

        // Oldest first, so a pass interrupted partway leaves a chronologically
        // contiguous history rather than a hole in the middle.
        for (log in remote.sortedBy { it.date }) {
            val row = log.toPlannedRow(uid)
            if (row == null) {
                decisions += Decision.Unusable(log.date, unusableReason(log))
                continue
            }
            when {
                row.date in ownedByUid -> {
                    // Held by this account already. Compare where we can: matching
                    // values are a no-op, differing values are a conflict to report
                    // rather than a row to overwrite.
                    val own = ownByDate[row.date]
                    decisions += if (own != null && !own.matches(row)) {
                        Decision.Conflict(row.date, own, row)
                    } else {
                        Decision.AlreadyOwned(row.date)
                    }
                }

                row.date in plannedDates ->
                    // Second remote record for a date already planned in this pass.
                    decisions += Decision.AlreadyOwned(row.date)

                else -> {
                    plannedDates += row.date
                    decisions += Decision.Insert(row, ownersByDate[row.date].orEmpty())
                }
            }
        }
        return decisions
    }

    /** Same night by the fields both sides actually store. */
    private fun OwnRow.matches(remote: PlannedRow): Boolean =
        sleepStart == remote.sleepStart &&
            sleepEnd == remote.sleepEnd &&
            totalSleepMinutes == remote.totalSleepMinutes

    private fun unusableReason(log: RemoteSleepLog): String = when {
        log.date.isNullOrBlank() -> "no date"
        log.source != null && log.source != "automatic" -> "source=${log.source}"
        log.sleepStart == null || log.sleepEnd == null -> "no detected sleep bounds"
        else -> "unusable sleep bounds"
    }

    /**
     * Converts a server record to a row for [uid], or null when it is not a usable
     * detected night.
     *
     * Manual entries are excluded: `sleep_sessions` holds nights the phone
     * detected, and a mood-only log has no session to reconstruct. Records written
     * before `source` existed are judged by the same sleepStart/sleepEnd-presence
     * fallback the server itself uses, so older nights stay recoverable.
     *
     * Duration prefers `sleepDuration`, which is the exact round-trip of the row's
     * own totalSleepMinutes (the phone writes `totalSleepMinutes / 60.0`), so
     * restoring through it reproduces the original rather than recomputing one.
     * The start→end span is a fallback only, and is a WIDER quantity: it includes
     * time awake during the night, which the detector excludes.
     */
    private fun RemoteSleepLog.toPlannedRow(uid: String): PlannedRow? {
        val date = date?.takeIf { it.isNotBlank() } ?: return null
        if (source != null && source != "automatic") return null
        val start = sleepStart ?: return null
        val end = sleepEnd ?: return null
        if (start <= 0L || end <= start) return null

        val minutes = sleepDuration
            ?.let { (it * 60).roundToInt() }
            ?: ((end - start) / 60_000L).toInt()
        if (minutes <= 0) return null

        return PlannedRow(
            date = date,
            // Always the restoring account, never anything read from the payload.
            ownerUid = uid,
            sleepStart = start,
            sleepEnd = end,
            totalSleepMinutes = minutes,
            // Absent fields restore as 0 rather than a guess. 0 is what the detector
            // records for an undisturbed night, so this reads as "nothing recorded"
            // instead of inventing awakenings.
            awakeningCount = awakeningCount ?: 0,
            sleepQuality = sleepQuality ?: 0,
            disturbanceScore = disturbanceScore ?: 0,
            remoteId = id
        )
    }
}
