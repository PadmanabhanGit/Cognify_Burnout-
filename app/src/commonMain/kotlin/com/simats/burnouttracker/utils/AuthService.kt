package com.simats.burnouttracker.utils

import androidx.compose.runtime.Composable

interface AuthService {
    fun isLoggedIn(): Boolean
    fun getCurrentUserEmail(): String?
    fun signOut()
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String, fullName: String): AuthResult
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult
    suspend fun resetPassword(email: String): AuthResult
}

data class AuthResult(
    val success: Boolean,
    val message: String? = null
)

@Composable
expect fun rememberAuthService(): AuthService

@Composable
expect fun GoogleSignInButton(
    onTokenReceived: (String) -> Unit,
    onFailure: (String) -> Unit
)
