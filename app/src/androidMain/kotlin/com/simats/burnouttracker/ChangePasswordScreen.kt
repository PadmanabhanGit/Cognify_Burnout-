package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.simats.burnouttracker.utils.rememberAuthService
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun ChangePasswordScreen(navController: NavController) {
    val authService = rememberAuthService()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }

    val headerGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF9333EA), Color(0xFF2563EB))
    )

    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF9333EA), Color(0xFF2563EB))
    )

    Scaffold(
        containerColor = ThemeColors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(headerGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { navController.popBackStack() },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Change Password",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ensure your account is using a strong password",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }

            // Form Card
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset(y = (-40).dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Security Details",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColors.textPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        // Current Password
                        PasswordFormField(
                            label = "Current Password",
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            placeholder = "Enter current password",
                            isVisible = currentVisible,
                            onToggleVisible = { currentVisible = !currentVisible }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // New Password
                        PasswordFormField(
                            label = "New Password",
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            placeholder = "Enter new password",
                            isVisible = newVisible,
                            onToggleVisible = { newVisible = !newVisible }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Confirm New Password
                        PasswordFormField(
                            label = "Confirm New Password",
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = "Re-type new password",
                            isVisible = confirmVisible,
                            onToggleVisible = { confirmVisible = !confirmVisible }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action Button
                Button(
                    onClick = { 
                        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isLoading = true
                        coroutineScope.launch {
                            val result = authService.changePassword(currentPassword, newPassword)
                            isLoading = false
                            if (result.success) {
                                Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } else {
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(buttonGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "Update Password",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun PasswordFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isVisible: Boolean,
    onToggleVisible: () -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ThemeColors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, color = ThemeColors.textTertiary, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { 
                Icon(
                    Icons.Default.Lock, 
                    contentDescription = null, 
                    tint = ThemeColors.textTertiary, 
                    modifier = Modifier.size(20.dp)
                ) 
            },
            trailingIcon = {
                IconButton(onClick = onToggleVisible) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = ThemeColors.textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                unfocusedContainerColor = ThemeColors.background,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = ThemeColors.border,
                focusedBorderColor = Color(0xFF9333EA)
            ),
            singleLine = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChangePasswordScreenPreview() {
    ChangePasswordScreen(rememberNavController())
}
