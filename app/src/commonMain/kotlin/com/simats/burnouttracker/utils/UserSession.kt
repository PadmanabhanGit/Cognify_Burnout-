package com.simats.burnouttracker.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The single canonical answer to "who is signed in right now".
 *
 * Before this existed, every component answered that question for itself — or,
 * far more often, did not ask it at all. Local caches were keyed by nothing,
 * background work captured no identity, and the UI rendered whatever the
 * previous account had left in memory. This object is the one place the current
 * Firebase UID is held, and the one thing every user-scoped read, write and
 * render is expected to consult.
 *
 * Two values matter:
 *
 *  - [uid] — the Firebase UID, or null when signed out. Never derive identity
 *    from an email, a display name, or a locally cached string; those are all
 *    mutable by the user and none of them is unique.
 *
 *  - [epoch] — a counter that increments on EVERY transition, including
 *    sign-out. It exists because "is the same user still signed in?" is not
 *    answerable from [uid] alone: A → B → A returns the same uid, yet any work
 *    started under the first A must not be allowed to land under the second.
 *    Comparing epochs makes that distinguishable.
 *
 * This class deliberately knows nothing about Firebase. The platform layer
 * pushes transitions in (see SessionLifecycle), which keeps it usable from
 * commonMain and trivially testable.
 */
object UserSession {

    /** Firebase UID of the signed-in account, or null when signed out. */
    var uid: String? by mutableStateOf(null)
        private set

    /**
     * Increments on every session transition. Capture it before starting async
     * work and re-check it before applying the result — see [isStillActive].
     */
    var epoch: Long by mutableStateOf(0L)
        private set

    /**
     * False until the session has finished initialising for [uid].
     *
     * The login gate depends on this: a screen must render a loading state
     * rather than a value while it is false, otherwise the first frame after
     * an account switch paints whatever the previous account left behind.
     */
    var isReady: Boolean by mutableStateOf(false)
        private set

    /** True when someone is signed in and their session has finished initialising. */
    val isActive: Boolean get() = uid != null && isReady

    /**
     * The current UID, or throws.
     *
     * Use this immediately before any user-scoped WRITE. Failing loudly is the
     * correct outcome: a write with no established owner is the bug that let one
     * account's data be persisted under another's identity.
     */
    fun requireUid(): String =
        uid ?: throw IllegalStateException(
            "No authenticated user. A user-scoped write was attempted while signed out."
        )

    /**
     * True when the session captured at the start of an operation is still the
     * live one.
     *
     * The guard every asynchronous user-scoped operation needs:
     * ```
     * val uid   = UserSession.requireUid()
     * val epoch = UserSession.epoch
     * val result = doSlowThing()
     * if (!UserSession.isStillActive(uid, epoch)) return   // account moved on; drop it
     * ```
     * Checking the uid alone is not sufficient (A → B → A), and checking the
     * epoch alone is not sufficient either if a caller passes a stale uid, so
     * both are compared.
     */
    fun isStillActive(capturedUid: String?, capturedEpoch: Long): Boolean =
        capturedEpoch == epoch && capturedUid != null && capturedUid == uid

    /**
     * Records that [newUid] is now the signed-in account.
     *
     * Returns true when this was an actual change of account, which is the
     * caller's signal to tear down and rebuild per-user state. Re-authenticating
     * as the same user returns false and bumps nothing, so a token refresh or a
     * redundant auth callback cannot spuriously invalidate live work.
     */
    fun beginSession(newUid: String): Boolean {
        val changed = uid != newUid
        if (changed) {
            uid = newUid
            epoch += 1
        }
        isReady = false
        return changed
    }

    /** Marks session initialisation complete. Until this runs, screens must not render values. */
    fun markReady() {
        isReady = true
    }

    /**
     * Records that no one is signed in.
     *
     * The epoch is bumped even though the uid becomes null, because in-flight
     * work started under the outgoing account must be invalidated at the moment
     * of sign-out — not merely when the next account arrives.
     */
    fun endSession() {
        uid = null
        epoch += 1
        isReady = false
    }
}
