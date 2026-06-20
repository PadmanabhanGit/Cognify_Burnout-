package com.simats.burnouttracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun TermsOfServiceScreen(navController: NavController) {
    var isAccepted by remember { mutableStateOf(false) }
    
    val headerGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF9333EA), Color(0xFF2563EB))
    )
    
    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF9333EA), Color(0xFF2563EB))
    )

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isAccepted = !isAccepted }
                    ) {
                        Checkbox(
                            checked = isAccepted,
                            onCheckedChange = { isAccepted = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF9333EA))
                        )
                        Text(
                            text = "I have read and agree to the Terms of Service.",
                            fontSize = 14.sp,
                            color = Color(0xFF4B5563)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { if (isAccepted) navController.popBackStack() },
                        enabled = isAccepted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (isAccepted) Modifier.background(buttonGradient) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Accept & Continue",
                                color = if (isAccepted) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(paddingValues)
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
                        text = "Terms of Service",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please review carefully before using Cognify",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }

            // Terms Content
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset(y = (-40).dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        TermsSection(
                            title = "Introduction",
                            content = "By using Cognify, you agree to these Terms of Service. These terms govern your use of the app and related services. If you do not agree to these terms, please do not use the application."
                        )
                        
                        TermsSection(
                            title = "User Responsibilities",
                            content = "You agree to provide accurate, current, and complete information when creating an account. You are responsible for maintaining the confidentiality of your login credentials and for all activities that occur under your account."
                        )
                        
                        TermsSection(
                            title = "Data Usage",
                            content = "Cognify collects and processes data related to your study habits, sleep patterns, and mood to provide personalized insights and burnout predictions. We prioritize your privacy and do not sell your personal information to third parties."
                        )
                        
                        TermsSection(
                            title = "Wellness Disclaimer",
                            content = "Cognify is designed to assist in monitoring productivity and mental wellness. It is not a substitute for professional medical advice, diagnosis, or treatment. Always consult with a qualified health professional for mental health concerns or before making changes to your health regimen."
                        )

                        TermsSection(
                            title = "Intellectual Property",
                            content = "All content, features, and functionality provided through Cognify are the exclusive property of Cognify and its licensors. You may not reproduce, distribute, or create derivative works without explicit permission."
                        )

                        TermsSection(
                            title = "Limitation of Liability",
                            content = "Cognify shall not be liable for any indirect, incidental, special, consequential, or punitive damages resulting from your use or inability to use the service."
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun TermsSection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = Color(0xFF4B5563)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TermsOfServiceScreenPreview() {
    TermsOfServiceScreen(rememberNavController())
}
