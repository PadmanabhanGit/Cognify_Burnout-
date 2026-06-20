package com.simats.burnouttracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.utils.rememberPlatformSettings
import com.simats.burnouttracker.utils.triggerActionPlanSync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralizedActionPlanScreen(navController: NavController) {
    val settings = rememberPlatformSettings("action_plan_settings")
    
    // Emerald/Green gradient for this screen
    val emeraldGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF10B981), Color(0xFF059669))
    )

    // State management for all settings
    var studyRemindersEnabled by remember { mutableStateOf(settings.getBoolean("study_reminders", true)) }
    var studyDuration by remember { mutableStateOf(settings.getString("study_duration", "45 min") ?: "45 min") }
    
    var breakAlertsEnabled by remember { mutableStateOf(settings.getBoolean("break_alerts", true)) }
    var breakDuration by remember { mutableStateOf(settings.getString("break_duration", "10 min") ?: "10 min") }

    var limitSocialEnabled by remember { mutableStateOf(settings.getBoolean("limit_social", false)) }
    var socialLimitMins by remember { mutableStateOf(settings.getInt("social_limit", 60).toFloat()) }

    var limitStreamingEnabled by remember { mutableStateOf(settings.getBoolean("limit_streaming", false)) }
    var streamingLimitMins by remember { mutableStateOf(settings.getInt("streaming_limit", 120).toFloat()) }

    var sleepReminderEnabled by remember { mutableStateOf(settings.getBoolean("sleep_reminder", true)) }
    var mindfulnessEnabled by remember { mutableStateOf(settings.getBoolean("mindfulness_reminder", true)) }
    var hydrationEnabled by remember { mutableStateOf(settings.getBoolean("hydration_reminder", true)) }

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
                    .height(180.dp)
                    .background(emeraldGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 40.dp, start = 20.dp, end = 20.dp)
            ) {
                Column {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Generalized Action\nPlan",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )
                    Text(
                        text = "Set reminders and limits to stay balanced",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-20).dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Study & Break Notifications
                ActionCard(
                    title = "Study & Break Notifications",
                    icon = Icons.Default.Timer,
                    iconColor = Color(0xFF6366F1)
                ) {
                    SettingToggleRow(
                        label = "Enable Study Session Reminders",
                        checked = studyRemindersEnabled,
                        onCheckedChange = { studyRemindersEnabled = it }
                    )
                    
                    if (studyRemindersEnabled) {
                        DurationPicker("STUDY DURATION", studyDuration, listOf("25 min", "45 min", "60 min", "90 min")) { studyDuration = it }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingToggleRow(
                        label = "Enable Break Alerts",
                        checked = breakAlertsEnabled,
                        onCheckedChange = { breakAlertsEnabled = it }
                    )
                    
                    if (breakAlertsEnabled) {
                        DurationPicker("BREAK DURATION", breakDuration, listOf("5 min", "10 min", "15 min", "20 min")) { breakDuration = it }
                    }
                }

                // 2. Entertainment Limits
                ActionCard(
                    title = "Entertainment Limits",
                    icon = Icons.Default.Block,
                    iconColor = Color(0xFFEF4444)
                ) {
                    Text(
                        text = "Set daily boundaries for apps to maintain focus.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LimitSlider(
                        label = "Limit Social Media Apps",
                        enabled = limitSocialEnabled,
                        onToggle = { limitSocialEnabled = it },
                        value = socialLimitMins,
                        onValueChange = { socialLimitMins = it },
                        max = 120f
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LimitSlider(
                        label = "Limit Streaming Apps",
                        enabled = limitStreamingEnabled,
                        onToggle = { limitStreamingEnabled = it },
                        value = streamingLimitMins,
                        onValueChange = { streamingLimitMins = it },
                        max = 180f
                    )
                }

                // 3. Wellness Reminder
                ActionCard(
                    title = "Wellness Reminder",
                    icon = Icons.Default.Spa,
                    iconColor = Color(0xFF10B981)
                ) {
                    WellnessItem("Sleep Reminder", "10:00 PM", sleepReminderEnabled, Icons.Default.Bedtime) { sleepReminderEnabled = it }
                    WellnessItem("Mindfulness", "9:00 AM", mindfulnessEnabled, Icons.Default.SelfImprovement) { mindfulnessEnabled = it }
                    WellnessItem("Hydration", "Every 2 hours", hydrationEnabled, Icons.Default.WaterDrop) { hydrationEnabled = it }
                }

                // Save Button
                Button(
                    onClick = {
                        // Actual Working Logic: Persist settings
                        settings.putBoolean("study_reminders", studyRemindersEnabled)
                        settings.putString("study_duration", studyDuration)
                        settings.putBoolean("break_alerts", breakAlertsEnabled)
                        settings.putString("break_duration", breakDuration)
                        settings.putBoolean("limit_social", limitSocialEnabled)
                        settings.putInt("social_limit", socialLimitMins.toInt())
                        settings.putBoolean("limit_streaming", limitStreamingEnabled)
                        settings.putInt("streaming_limit", streamingLimitMins.toInt())
                        settings.putBoolean("sleep_reminder", sleepReminderEnabled)
                        settings.putBoolean("mindfulness_reminder", mindfulnessEnabled)
                        settings.putBoolean("hydration_reminder", hydrationEnabled)
                        
                        // Actual Working Logic: Re-trigger background notifications
                        triggerActionPlanSync()

                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF3B82F6))),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Save Action Plan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ActionCard(title: String, icon: ImageVector, iconColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = iconColor.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
            }
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun SettingToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF10B981)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationPicker(label: String, current: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Surface(
            color = Color(0xFFF9FAFB),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = current, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
        }
        
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun LimitSlider(label: String, enabled: Boolean, onToggle: (Boolean) -> Unit, value: Float, onValueChange: (Float) -> Unit, max: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFEF4444))
            )
        }
        
        if (enabled) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Daily Limit", fontSize = 11.sp, color = Color.Gray)
                Text(text = "${value.toInt()} min", fontSize = 11.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..max,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF6366F1), activeTrackColor = Color(0xFF6366F1))
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0m", fontSize = 10.sp, color = Color.Gray)
                Text("${max.toInt()}m", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun WellnessItem(label: String, subtitle: String, checked: Boolean, icon: ImageVector, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
            Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF10B981))
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF10B981))
        )
    }
}
