package com.simats.burnouttracker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun LinkedAccountsScreen(navController: NavController) {
    val headerGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF9333EA), Color(0xFF2563EB))
    )

    Scaffold(
        containerColor = Color(0xFFF9FAFB)
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
                        text = "Linked Accounts",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect your social accounts for easier access",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }

            // Linked Accounts List
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Connect Accounts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            modifier = Modifier.padding(8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        AccountLinkItem(
                            icon = R.drawable.ic_google,
                            name = "Google",
                            status = "Linked",
                            statusColor = Color(0xFF16A34A),
                            onClick = { /* Handle Google link/unlink */ }
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = Color(0xFFF3F4F6))
                        
                        AccountLinkItem(
                            icon = R.drawable.ic_facebook,
                            name = "Facebook",
                            status = "Not Linked",
                            statusColor = Color(0xFF9CA3AF),
                            onClick = { /* Handle Facebook link/unlink */ }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = Color(0xFFF3F4F6))

                        AccountLinkItem(
                            icon = null, // Placeholder for LinkedIn since drawable is missing
                            name = "LinkedIn",
                            status = "Not Linked",
                            statusColor = Color(0xFF9CA3AF),
                            onClick = { /* Handle LinkedIn link/unlink */ }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Info Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEEF2FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7D2FE))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Linking your accounts allows you to sign in faster and sync your data across devices securely.",
                            fontSize = 13.sp,
                            color = Color(0xFF4338CA),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountLinkItem(
    icon: Int?,
    name: String,
    status: String,
    statusColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = Color(0xFFF9FAFB),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3F4F6))
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (icon != null) {
                    Image(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    // Fallback icon for LinkedIn or missing assets
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
            Text(
                text = status,
                fontSize = 13.sp,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LinkedAccountsScreenPreview() {
    LinkedAccountsScreen(rememberNavController())
}
