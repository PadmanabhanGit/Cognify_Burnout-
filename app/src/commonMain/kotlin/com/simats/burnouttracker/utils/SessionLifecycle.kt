package com.simats.burnouttracker.utils

/**
 * Establishes the session for [uid], synchronously.
 *
 * Must be called and allowed to RETURN before anything user-scoped is written
 * and before navigating away from the auth screen. That ordering is the whole
 * point: previously the account-change hook ran from a Firebase auth-state
 * callback while the login coroutine was concurrently writing the new user's
 * name, and nothing ordered the two — so the freshly written name could be
 * wiped by the clean-up for the account that had just been left.
 *
 * Idempotent. Re-establishing the session for the account that is already
 * active does nothing and does not disturb work in flight.
 */
expect fun beginUserSession(uid: String)

/**
 * Tears the session down completely, then leaves the caller to sign out.
 *
 * Stops background work, cancels scheduled alarms, and drops all in-memory
 * user state. Deliberately does NOT call signOut() itself — teardown needs a
 * valid identity to know what it is tearing down, so the caller signs out
 * afterwards.
 */
expect fun endUserSession()
