package com.simats.burnouttracker

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

import com.simats.burnouttracker.utils.rememberAuthService

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val authService = rememberAuthService()
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    // State variables
    var isDarkMode by remember { mutableStateOf(prefs.getBoolean("isDarkMode", false)) }
    var allowAllNotif by remember { mutableStateOf(prefs.getBoolean("allowAllNotif", true)) }
    var burnoutAlerts by remember { mutableStateOf(prefs.getBoolean("burnoutAlerts", true)) }
    var dailyReminders by remember { mutableStateOf(prefs.getBoolean("dailyReminders", true)) }
    var weeklyReports by remember { mutableStateOf(prefs.getBoolean("weeklyReports", false)) }
    var studyPrompts by remember { mutableStateOf(prefs.getBoolean("studyPrompts", true)) }
    var syncHealth by remember { mutableStateOf(prefs.getBoolean("syncHealth", false)) }

    val primaryPurple = Color(0xFF9333EA)
    val headerGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF4F46E5), Color(0xFF9333EA))
    )

    val backgroundColor = Color(0xFFF9FAFB)
    val cardColor = Color.White
    val textColor = Color(0xFF1F2937)
    val secondaryTextColor = Color(0xFF6B7280)
    val sectionTitleColor = Color(0xFF94A3B8)

    Scaffold(
        bottomBar = { AppBottomNavigation(navController, "settings") },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(headerGradient)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Text(
                        text = "Settings",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Profile Card (Overlapping)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-40).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = cardColor,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_background), 
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = CircleShape,
                                color = primaryPurple,
                                border = androidx.compose.foundation.BorderStroke(2.dp, cardColor)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = Color.White,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = authService.getCurrentUserEmail()?.split("@")?.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "User",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Computer Science Junior",
                            fontSize = 14.sp,
                            color = secondaryTextColor
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            onClick = { navController.navigate("personal_information") },
                            shape = RoundedCornerShape(20.dp),
                            color = primaryPurple.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Edit Profile",
                                color = primaryPurple,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ACCOUNT SECTION
                SettingsGroup(title = "ACCOUNT", cardColor = cardColor, titleColor = sectionTitleColor) {
                    SettingsItem(
                        icon = Icons.Outlined.Person,
                        title = "Personal Information",
                        textColor = textColor,
                        iconColor = Color(0xFFC084FC)
                    ) { navController.navigate("personal_information") }
                    
                    SettingsItem(
                        icon = Icons.Outlined.Link,
                        title = "Linked Accounts",
                        trailingText = "Google",
                        textColor = textColor,
                        iconColor = Color(0xFFC084FC)
                    ) { navController.navigate("linked_accounts") }
                    
                    SettingsItem(
                        icon = Icons.Outlined.Lock,
                        title = "Change Password",
                        textColor = textColor,
                        iconColor = Color(0xFFC084FC)
                    ) { navController.navigate("change_password") }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // NOTIFICATION PREFERENCES
                SettingsGroup(title = "NOTIFICATION PREFERENCES", cardColor = cardColor, titleColor = sectionTitleColor) {
                    SettingsToggleItem(
                        icon = Icons.Outlined.NotificationsActive,
                        title = "Allow All Notifications",
                        checked = allowAllNotif,
                        onCheckedChange = { allowAllNotif = it },
                        textColor = textColor,
                        primaryColor = primaryPurple
                    )
                    
                    SettingsToggleItem(
                        title = "Burnout Alerts",
                        subtitle = "AI predictions based on stress",
                        checked = burnoutAlerts,
                        onCheckedChange = { burnoutAlerts = it },
                        textColor = textColor,
                        primaryColor = primaryPurple
                    )
                    
                    SettingsToggleItem(
                        title = "Daily Reminders",
                        checked = dailyReminders,
                        onCheckedChange = { dailyReminders = it },
                        textColor = textColor,
                        primaryColor = primaryPurple
                    )
                    
                    SettingsToggleItem(
                        title = "Weekly Reports",
                        checked = weeklyReports,
                        onCheckedChange = { weeklyReports = it },
                        textColor = textColor,
                        primaryColor = primaryPurple
                    )
                    
                    SettingsToggleItem(
                        title = "Study Session Prompts",
                        checked = studyPrompts,
                        onCheckedChange = { studyPrompts = it },
                        textColor = textColor,
                        primaryColor = primaryPurple
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // WELLNESS
                SettingsGroup(title = "WELLNESS", cardColor = cardColor, titleColor = sectionTitleColor) {
                    SettingsToggleItem(
                        icon = Icons.Outlined.HealthAndSafety,
                        title = "Sync with Health Apps",
                        checked = syncHealth,
                        onCheckedChange = { syncHealth = it },
                        textColor = textColor,
                        primaryColor = Color(0xFF22C55E)
                    )
                    
                    SettingsItem(
                        icon = Icons.Outlined.Shield,
                        title = "Privacy & Data",
                        textColor = textColor,
                        iconColor = Color(0xFF3B82F6)
                    ) { navController.navigate("privacy_data") }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // APP PREFERENCES
                SettingsGroup(title = "APP PREFERENCES", cardColor = cardColor, titleColor = sectionTitleColor) {
                    SettingsToggleItem(
                        icon = Icons.Outlined.DarkMode,
                        title = "Dark Mode",
                        checked = isDarkMode,
                        onCheckedChange = { isDarkMode = it },
                        textColor = textColor,
                        primaryColor = Color(0xFF1F2937)
                    )
                    
                    SettingsItem(
                        icon = Icons.Outlined.Public,
                        title = "Language",
                        trailingText = "English (US)",
                        textColor = textColor,
                        iconColor = Color(0xFF64748B)
                    ) { /* Navigate */ }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SUPPORT
                SettingsGroup(title = "SUPPORT", cardColor = cardColor, titleColor = sectionTitleColor) {
                    SettingsItem(
                        title = "Help Center",
                        trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                        textColor = textColor
                    ) { /* Navigate */ }
                    
                    SettingsItem(
                        title = "Terms of Service",
                        textColor = textColor
                    ) { navController.navigate("terms_of_service") }
                    
                    SettingsItem(
                        title = "Privacy Policy",
                        textColor = textColor
                    ) { navController.navigate("privacy_policy") }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Log Out Button Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            authService.signOut()
                            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                            ).build()
                            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                            googleSignInClient.signOut().addOnCompleteListener {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = cardColor,
                    shadowElevation = 2.dp
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Log Out",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Cognify v2.4.1",
                    color = secondaryTextColor,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String, 
    cardColor: Color, 
    titleColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = cardColor,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
    textColor: Color = Color(0xFF1F2937),
    iconColor: Color = Color(0xFF8B5CF6),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 11.sp, color = Color.Gray)
            }
        }
        if (trailingText != null) {
            Text(text = trailingText, fontSize = 13.sp, color = Color.LightGray, modifier = Modifier.padding(end = 4.dp))
        }
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color = Color(0xFF1F2937),
    primaryColor: Color = Color(0xFF8B5CF6)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 11.sp, color = Color.Gray)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = primaryColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE2E8F0),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(rememberNavController())
}
