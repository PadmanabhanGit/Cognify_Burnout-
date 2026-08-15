package com.simats.burnouttracker.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The signed-in user's name, in one place.
 *
 * OWNERSHIP MODEL
 * `users/{uid}` in Firestore is canonical. Everything here — and the local
 * SharedPreferences cache behind it, and Firebase Auth's displayName — is a
 * derived cache of that document, refreshed on session start and after a save.
 * Nothing reads a name from anywhere else.
 *
 * WHY THIS EXISTS
 * The name previously lived in five places that drifted apart: Firestore
 * `firstName`/`lastName`, a separate Firestore `fullName` that profile saves
 * never updated, Firebase Auth displayName (updated fire-and-forget), a
 * `firstName` pref, and a `fullName` pref written only at registration. Editing
 * your profile updated some of them, so the Dashboard, the Settings header and
 * the web app could each show a different name for the same person.
 *
 * THE REVERT RACE
 * The Dashboard polls GET /api/dashboard every 30s and used to write the name it
 * received straight into the local cache. A response already in flight when the
 * user pressed Save would land afterwards and overwrite the new name with the
 * old one. [revision] closes that: a server value is only accepted if no local
 * edit happened while the request was outstanding. See [applyFromServer].
 */
object UserProfile {

    private const val KEY_FIRST = "firstName"
    private const val KEY_LAST = "lastName"

    var firstName: String? by mutableStateOf(null)
        private set

    var lastName: String? by mutableStateOf(null)
        private set

    /**
     * Bumped on every locally-originated edit. Capture it before an async fetch
     * and pass it to [applyFromServer] so a slower server response cannot
     * clobber a newer local truth.
     */
    var revision: Long by mutableStateOf(0L)
        private set

    /** "First Last", or null when neither part is set. Never a fabricated placeholder. */
    val fullName: String?
        get() = listOfNotNull(firstName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .takeIf { it.isNotBlank() }

    /**
     * The greeting name, with an explicit fallback chain:
     * profile first name → the email's local part → "Student".
     *
     * The email fallback is a display convenience only. It is never persisted as
     * a name and never written back to the server, so it cannot masquerade as
     * one the user actually chose.
     */
    fun greetingName(email: String?): String =
        firstName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore('@')?.replaceFirstChar { it.uppercase() }?.takeIf { it.isNotBlank() }
            ?: "Student"

    /**
     * Applies a name the user just entered, and bumps [revision].
     *
     * Call this only after the canonical Firestore write has succeeded — the
     * cache must never claim a name the server rejected.
     */
    fun applyLocalEdit(first: String?, last: String?) {
        firstName = first?.trim()?.takeIf { it.isNotBlank() }
        lastName = last?.trim()?.takeIf { it.isNotBlank() }
        revision += 1
        persist()
    }

    /**
     * Applies a name fetched from the server, but only if no local edit happened
     * since [expectedRevision] was captured.
     *
     * Returns true when the value was applied. A false return is not an error —
     * it means a newer local edit won, which is the correct outcome.
     */
    fun applyFromServer(first: String?, last: String?, expectedRevision: Long): Boolean {
        if (revision != expectedRevision) return false
        val newFirst = first?.trim()?.takeIf { it.isNotBlank() }
        val newLast = last?.trim()?.takeIf { it.isNotBlank() }
        if (newFirst == firstName && newLast == lastName) return true
        firstName = newFirst
        lastName = newLast
        persist()
        return true
    }

    /**
     * Repopulates from the local per-user cache.
     *
     * Called during session initialisation so a name is available on the first
     * frame after a cold start, before the network round-trip completes. The
     * store this reads is itself scoped to the active account, so it cannot
     * surface a different user's name.
     */
    fun loadFromCache() {
        val settings = platformSettings()
        firstName = settings.getString(KEY_FIRST, null)?.takeIf { it.isNotBlank() }
        lastName = settings.getString(KEY_LAST, null)?.takeIf { it.isNotBlank() }
    }

    /** Wipes in-memory name state. Used on sign-out and on account change. */
    fun clear() {
        firstName = null
        lastName = null
        revision = 0L
    }

    private fun persist() {
        val settings = platformSettings()
        firstName?.let { settings.putString(KEY_FIRST, it) } ?: settings.remove(KEY_FIRST)
        lastName?.let { settings.putString(KEY_LAST, it) } ?: settings.remove(KEY_LAST)
    }
}
