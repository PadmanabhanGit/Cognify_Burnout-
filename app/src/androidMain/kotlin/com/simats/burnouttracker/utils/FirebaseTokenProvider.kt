package com.simats.burnouttracker.utils

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

actual object FirebaseTokenProvider {
    actual suspend fun getIdToken(): String? {
        return try {
            FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Read from FirebaseAuth rather than from AccountScope's persisted uid.
     *
     * They are the same value in steady state but diverge exactly when it
     * matters: during sign-out FirebaseAuth clears first, and during sign-in it
     * is populated before AccountScope has synced. Taking the live auth value
     * means an ownership check made in either of those windows fails closed —
     * nothing is stopped under an identity that is on its way out or not yet
     * established.
     */
    actual fun currentUid(): String =
        try {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        } catch (e: Exception) {
            ""
        }
}
