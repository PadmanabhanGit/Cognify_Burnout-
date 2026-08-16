package com.simats.burnouttracker.utils

expect object FirebaseTokenProvider {
    suspend fun getIdToken(): String?

    /**
     * The uid of the account currently signed in, or "" when signed out.
     *
     * The authorization side of ownership: every study-session stop compares the
     * session's recorded owner against THIS, not against whichever settings file
     * happened to resolve. "" is the signed-out sentinel and matches no owner, so
     * a signed-out app can stop nothing.
     */
    fun currentUid(): String
}
