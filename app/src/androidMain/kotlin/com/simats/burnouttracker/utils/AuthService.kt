package com.simats.burnouttracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.simats.burnouttracker.R
import com.simats.burnouttracker.data.api.RetrofitClient
import com.simats.burnouttracker.data.models.RegisterRequest
import kotlinx.coroutines.tasks.await

@Composable
actual fun rememberAuthService(): AuthService {
    return remember { AndroidAuthService() }
}

class AndroidAuthService : AuthService {
    private val auth = FirebaseAuth.getInstance()

    override fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    override fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    override fun signOut() {
        auth.signOut()
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            AuthResult(true)
        } catch (e: Exception) {
            AuthResult(false, e.message ?: "Login failed")
        }
    }

    override suspend fun signUp(email: String, password: String, fullName: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            result.user?.updateProfile(profileUpdates)?.await()

            // Create the profile via backend
            val apiService = RetrofitClient.getApiService()
            val response = apiService.register(RegisterRequest(email = email, password = password, fullName = fullName))

            if (!response.isSuccessful) {
                return AuthResult(false, "Account created but profile setup failed")
            }

            AuthResult(true)
        } catch (e: Exception) {
            AuthResult(false, e.message ?: "Registration failed")
        }
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            AuthResult(true)
        } catch (e: Exception) {
            AuthResult(false, e.message ?: "Google Sign-In failed")
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult {
        val user = auth.currentUser ?: return AuthResult(false, "User not logged in")
        val email = user.email ?: return AuthResult(false, "No email associated with this account (possibly Google Auth)")

        return try {
            // Re-authenticate first
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            // Then update password
            user.updatePassword(newPassword).await()
            AuthResult(true)
        } catch (e: Exception) {
            AuthResult(false, e.message ?: "Failed to update password")
        }
    }

    override suspend fun resetPassword(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthResult(true)
        } catch (e: Exception) {
            AuthResult(false, e.message ?: "Failed to send reset email")
        }
    }
}

@Composable
actual fun GoogleSignInButton(
    onTokenReceived: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    val context = LocalContext.current
    
    // Note: You need to replace this with your actual Web Client ID from Firebase Console
    val webClientId = "966389564228-vq6558vla737es6aqu6l61bqqjg2u1ar.apps.googleusercontent.com"

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                onTokenReceived(idToken)
            } else {
                onFailure("ID Token is null")
            }
        } catch (e: ApiException) {
            onFailure(e.message ?: "Google Sign-In failed")
        }
    }

    OutlinedButton(
        onClick = { launcher.launch(googleSignInClient.signInIntent) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Using a simple image placeholder for Google logo if resource not found
            // In a real app, use painterResource(R.drawable.ic_google_logo)
            Text("G", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Google", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
