package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.utils.rememberPlatformSettings

@Composable
fun PrivacyPolicyScreen(navController: NavController, isFirstTime: Boolean = false) {
    val settings = rememberPlatformSettings()
    var isAccepted by remember { mutableStateOf(false) }

    val primaryGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF9333EA), Color(0xFF4F46E5))
    )

    Scaffold(
        bottomBar = {
            if (isFirstTime) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .navigationBarsPadding()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Checkbox(
                                checked = isAccepted,
                                onCheckedChange = { isAccepted = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF9333EA))
                            )
                            Text(
                                text = "I have read and agree to the Privacy Policy",
                                fontSize = 14.sp,
                                color = Color(0xFF374151)
                            )
                        }
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable(enabled = isAccepted) {
                                    settings.putBoolean("policy_accepted", true)
                                    navController.navigate("login") {
                                        popUpTo("onboarding_privacy") { inclusive = true }
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isAccepted) Color.Transparent else ThemeColors.border
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(if (isAccepted) primaryGradient else Brush.linearGradient(listOf(ThemeColors.border, ThemeColors.border)))
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Continue",
                                    color = if (isAccepted) Color.White else ThemeColors.textTertiary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColors.background)
                .padding(paddingValues)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 48.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    if (!isFirstTime) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                    Text(
                        text = "Privacy Policy",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Last updated: October 2024",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                PrivacySection(
                    icon = Icons.Default.Shield,
                    title = "Your Privacy Matters",
                    content = "At Cognify, we take your privacy seriously. This policy explains how we collect, use, and protect your personal information and usage data."
                )

                PrivacySection(
                    icon = Icons.Default.DataUsage,
                    title = "Information We Collect",
                    content = buildAnnotatedString {
                        append("• ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("Personal Info: ") }
                        append("Name, email address, and profile details you provide.\n\n")
                        append("• ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("Usage Data: ") }
                        append("App usage statistics to analyze productivity and burnout risk.\n\n")
                        append("• ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("Wellness Logs: ") }
                        append("Sleep patterns and mood assessments you record.")
                    }
                )

                PrivacySection(
                    icon = Icons.Default.Lock,
                    title = "How We Use Data",
                    content = "Your data is used solely to provide personalized burnout risk assessments, productivity insights, and to improve our AI models. We do not sell your personal information to third parties."
                )

                PrivacySection(
                    icon = Icons.Default.Security,
                    title = "Data Security",
                    content = "We implement industry-standard security measures to protect your data from unauthorized access, alteration, or disclosure."
                )

                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "If you have any questions about this Privacy Policy, please contact us at privacy@simats.com",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PrivacySection(icon: ImageVector, title: String, content: Any) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color(0xFFF3E8FF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeColors.textPrimary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (content is String) {
            Text(
                text = content,
                fontSize = 15.sp,
                color = ThemeColors.textSecondary,
                lineHeight = 24.sp
            )
        } else if (content is androidx.compose.ui.text.AnnotatedString) {
            Text(
                text = content,
                fontSize = 15.sp,
                color = ThemeColors.textSecondary,
                lineHeight = 24.sp
            )
        }
    }
}
