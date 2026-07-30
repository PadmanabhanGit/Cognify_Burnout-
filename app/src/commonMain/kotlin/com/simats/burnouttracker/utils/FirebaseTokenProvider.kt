package com.simats.burnouttracker.utils

expect object FirebaseTokenProvider {
    suspend fun getIdToken(): String?
}
