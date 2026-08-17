package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.simats.burnouttracker.utils.AppData
import com.simats.burnouttracker.utils.UserProfile
import kotlinx.coroutines.launch
import com.simats.burnouttracker.data.api.RetrofitClient
import com.simats.burnouttracker.data.models.ProfileData

@Composable
fun PrivacyDataScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    // Initialize global privacy settings from prefs
    LaunchedEffect(Unit) {
        AppData.anonymousAnalytics = prefs.getBoolean("anonymousAnalytics", false)
        AppData.personalizedInsights = prefs.getBoolean("personalizedInsights", true)
    }

    val scope = rememberCoroutineScope()

    // Save function
    val saveSettings = {
        with(prefs.edit()) {
            putBoolean("syncHealth", AppData.syncHealth)
            putBoolean("anonymousAnalytics", AppData.anonymousAnalytics)
            putBoolean("personalizedInsights", AppData.personalizedInsights)
            apply()
        }
        
        scope.launch {
            try {
                RetrofitClient.getApiService().updateProfileInfo(
                    ProfileData(
                        syncHealth = AppData.syncHealth,
                        anonymousAnalytics = AppData.anonymousAnalytics,
                        personalizedInsights = AppData.personalizedInsights
                    )
                )
                Toast.makeText(context, "Preferences Saved and Synced!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Saved locally (Sync failed)", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val data = """
                        {
                            "user": "${UserProfile.fullName ?: "Anonymous"}",
                            "preferences": {
                                "syncHealth": ${AppData.syncHealth},
                                "anonymousAnalytics": ${AppData.anonymousAnalytics},
                                "personalizedInsights": ${AppData.personalizedInsights}
                            },
                            "burnoutPredictedScore": ${AppData.predictedScore}
                        }
                    """.trimIndent()
                    outputStream.write(data.toByteArray())
                }
                Toast.makeText(context, "Data successfully downloaded", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to download data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val indigoPurpleGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF9333EA), Color(0xFF2563EB))
    )

    Scaffold(
        bottomBar = { AppBottomNavigation(navController, "settings") },
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
                    .background(indigoPurpleGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
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
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Privacy & Data",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Control how your information is used",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset(y = (-40).dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Card 1: Data Sharing
                PrivacyCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Data Sharing",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColors.textPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    PrivacyToggleItem(
                        title = "Sync with Health Apps",
                        subtitle = "Connect metrics with Health Connect",
                        checked = AppData.syncHealth,
                        onCheckedChange = { AppData.syncHealth = it }
                    )
                    
                    PrivacyToggleItem(
                        title = "Allow Anonymous Analytics",
                        subtitle = "Help improve the app using anonymous data",
                        checked = AppData.anonymousAnalytics,
                        onCheckedChange = { AppData.anonymousAnalytics = it }
                    )
                    
                    PrivacyToggleItem(
                        title = "Enable Personalized Insights",
                        subtitle = "Get recommendations based on your habits",
                        checked = AppData.personalizedInsights,
                        onCheckedChange = { AppData.personalizedInsights = it }
                    )
                }

                // Card 2: Privacy Controls
                PrivacyCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Privacy Controls",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColors.textPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    PrivacyActionItem(
                        icon = Icons.Default.FileDownload,
                        title = "Download My Data",
                        onClick = { exportLauncher.launch("MyBurnoutData.json") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Preferences Button
                Button(
                    onClick = {
                        saveSettings()
                        navController.popBackStack()
                    },
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
                            .background(indigoPurpleGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Save Preferences",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
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
fun PrivacyCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            content()
        }
    }
}

@Composable
fun PrivacyToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ThemeColors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ThemeColors.textSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF9333EA),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE2E8F0),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun PrivacyActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color(0xFFEEF2FF)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF4F46E5),
                modifier = Modifier.padding(10.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = ThemeColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ThemeColors.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PrivacyDataScreenPreview() {
    PrivacyDataScreen(rememberNavController())
}
