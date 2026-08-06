package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch

import com.simats.burnouttracker.data.api.ApiClient
import com.simats.burnouttracker.data.models.LoginRequest
import com.simats.burnouttracker.utils.rememberAuthService
import com.simats.burnouttracker.utils.rememberPlatformSettings
import com.simats.burnouttracker.utils.GoogleSignInButton

@Composable
fun LoginScreen(
    onSignInClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val authService = rememberAuthService()
    val settings = rememberPlatformSettings()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }

    val handleSignIn = {
        scope.launch {
            if (email.isBlank() || password.isBlank()) {
                errorMessage = "Please enter email and password"
                return@launch
            }
            isLoading = true
            errorMessage = null
            val result = authService.signIn(email, password)
            if (result.success) {
                // Save user info to settings for Dashboard
                val firstName = authService.getCurrentUserEmail()?.split("@")?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Student"
                settings.putString("firstName", firstName)
                onSignInClick()
            } else {
                errorMessage = result.message
            }
            isLoading = false
        }
    }
    
    val handleGoogleSignIn = { idToken: String ->
        scope.launch {
            isLoading = true
            val result = authService.signInWithGoogle(idToken)
            if (result.success) {
                val firstName = authService.getCurrentUserEmail()?.split("@")?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Student"
                settings.putString("firstName", firstName)
                onSignInClick()
            } else {
                errorMessage = result.message
            }
            isLoading = false
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
    )
    
    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF9333EA), Color(0xFF6366F1))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Icon Placeholder (STT style)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Cognify",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Mental Health & Burnout Detection",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // White Card - Responsive width
            Surface(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(48.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            it,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.testTag("loginErrorMessage")
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Email Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Email Address",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("student@example.com", color = ThemeColors.textTertiary) },
                            modifier = Modifier.fillMaxWidth().testTag("loginEmailField"),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Rounded.Mail, 
                                    contentDescription = null, 
                                    tint = ThemeColors.textTertiary,
                                    modifier = Modifier.size(20.dp)
                                ) 
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1E293B),
                                unfocusedTextColor = Color(0xFF1E293B),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = Color(0xFF9333EA)
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Password Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Password",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("********", color = ThemeColors.textTertiary) },
                            modifier = Modifier.fillMaxWidth().testTag("loginPasswordField"),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Rounded.Lock, 
                                    contentDescription = null, 
                                    tint = ThemeColors.textTertiary,
                                    modifier = Modifier.size(20.dp)
                                ) 
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = ThemeColors.textTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1E293B),
                                unfocusedTextColor = Color(0xFF1E293B),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = Color(0xFF9333EA)
                            ),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showForgotPasswordDialog = true }) {
                            Text("Forgot Password?", color = Color(0xFF9333EA), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign In Button
                    Button(
                        onClick = { handleSignIn() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("loginButton"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(buttonGradient, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Sign In", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Divider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        Text(
                            text = "OR CONTINUE WITH",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = ThemeColors.textTertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    GoogleSignInButton(
                        onTokenReceived = { handleGoogleSignIn(it) },
                        onFailure = { errorMessage = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sign Up link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Don't have an account? ", color = Color.White.copy(alpha = 0.8f))
                Text(
                    text = "Sign Up",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSignUpClick() }.testTag("signUpLink")
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }

        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showForgotPasswordDialog = false 
                    resetMessage = null
                },
                title = { Text("Reset Password") },
                text = {
                    Column {
                        Text("Enter your email address to receive a password reset link.", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            placeholder = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        if (resetMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(resetMessage!!, color = if (resetMessage!!.contains("sent", ignoreCase = true)) Color(0xFF10B981) else Color.Red, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (resetEmail.isBlank()) {
                                resetMessage = "Please enter an email address"
                            } else {
                                scope.launch {
                                    val result = authService.resetPassword(resetEmail)
                                    if (result.success) {
                                        resetMessage = "Password reset email sent!"
                                    } else {
                                        resetMessage = result.message ?: "Failed to send reset email"
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Send Link", color = Color(0xFF9333EA), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPasswordDialog = false; resetMessage = null }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}
