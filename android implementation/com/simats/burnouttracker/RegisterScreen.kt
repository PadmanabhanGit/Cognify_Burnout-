package com.simats.burnouttracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag

import com.simats.burnouttracker.data.api.ApiClient
import com.simats.burnouttracker.data.models.RegisterRequest
import com.simats.burnouttracker.utils.rememberAuthService
import com.simats.burnouttracker.utils.rememberPlatformSettings
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(onSignInClick: () -> Unit = {}, onSignUpClick: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val settings = rememberPlatformSettings()
    val authService = rememberAuthService()
    
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val handleSignUp = {
        scope.launch {
            if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
                errorMessage = "Please fill all fields"
                return@launch
            }
            if (!agreeToTerms) {
                errorMessage = "Please agree to the terms"
                return@launch
            }
            if (confirmPassword != password) {
                errorMessage = "Passwords do not match"
                return@launch
            }
            isLoading = true
            errorMessage = null
            try {
                val result = authService.signUp(email, password, fullName)
                if (result.success) {
                    settings.putString("firstName", fullName.split(" ").firstOrNull() ?: "Student")
                    settings.putString("fullName", fullName)
                    settings.putString("email", email)
                    onSignUpClick()
                } else {
                    errorMessage = result.message
                }
            } catch (e: Exception) {
                errorMessage = "An unexpected error occurred"
            } finally {
                isLoading = false
            }
        }
    }
    
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF9333EA), Color(0xFF2563EB))
    )
    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF9333EA), Color(0xFF6366F1))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo Placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Logo", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Create Account",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Join Cognify to track your mental wellness",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // White Card
            Surface(
                modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (errorMessage != null) {
                        Text(
                            errorMessage!!,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.testTag("errorMessage")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Full Name Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Full Name",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            placeholder = { Text("John Doe", color = Color.LightGray) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray) },
                            modifier = Modifier.fillMaxWidth().testTag("fullNameField"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedBorderColor = Color(0xFF9333EA)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Email Address",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("student@example.com", color = Color.LightGray) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.LightGray) },
                            modifier = Modifier.fillMaxWidth().testTag("emailField"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedBorderColor = Color(0xFF9333EA)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Password",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("********", color = Color.LightGray) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.LightGray) },
                            trailingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.LightGray) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("passwordField"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedBorderColor = Color(0xFF9333EA)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Confirm Password",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = { Text("********", color = Color.LightGray) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.LightGray) },
                            trailingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.LightGray) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("confirmPasswordField"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedBorderColor = Color(0xFF9333EA)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Terms Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreeToTerms,
                            onCheckedChange = { agreeToTerms = it },
                            modifier = Modifier.testTag("termsCheckbox"),
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF9333EA))
                        )
                        Text(
                            text = buildAnnotatedString {
                                append("I agree to the ")
                                withStyle(style = SpanStyle(color = Color(0xFF9333EA), fontWeight = FontWeight.Bold)) {
                                    append("Terms & Conditions")
                                }
                                append(" and ")
                                withStyle(style = SpanStyle(color = Color(0xFF9333EA), fontWeight = FontWeight.Bold)) {
                                    append("Privacy Policy")
                                }
                            },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Create Account Button
                    Button(
                        onClick = { handleSignUp() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("registerButton"),
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
                                Text("Create Account", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Already have account
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Already have an account? ", color = Color.Gray, fontSize = 14.sp)
                        TextButton(onClick = onSignInClick) {
                            Text("Sign In", color = Color(0xFF9333EA), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
