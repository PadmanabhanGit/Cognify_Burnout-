package com.simats.burnouttracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAuthService(): AuthService {
    return remember { WebAuthService() }
}

class WebAuthService : AuthService {
    override fun isLoggedIn(): Boolean = false
    override fun getCurrentUserEmail(): String? = null
    override fun signOut() {}
    override suspend fun signIn(email: String, password: String): AuthResult = AuthResult(false, "Web Auth not implemented")
    override suspend fun signUp(email: String, password: String, fullName: String): AuthResult = AuthResult(false, "Web Auth not implemented")
    override suspend fun signInWithGoogle(idToken: String): AuthResult = AuthResult(false, "Google Sign-In not implemented for Web")
}

@Composable
actual fun GoogleSignInButton(
    onTokenReceived: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    // Mock for now
}
