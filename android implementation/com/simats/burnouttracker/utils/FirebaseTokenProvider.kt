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
}
