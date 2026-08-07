package com.simats.burnouttracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.ui.theme.ThemeColors
import com.simats.burnouttracker.utils.rememberPlatformSettings
import com.simats.burnouttracker.utils.triggerActionPlanSync
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralizedActionPlanScreen(navController: NavController) {
    // IMPORTANT: ActionPlanScheduler uses "action_plan" 
    val settings = rememberPlatformSettings("action_plan")
    
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

    // Wellness States
    var sleepReminderEnabled by remember { mutableStateOf(settings.getBoolean("sleepReminderEnabled", true)) }
    var sleepHour by remember { mutableStateOf(settings.getInt("sleepHour", 22)) }
    var sleepMinute by remember { mutableStateOf(settings.getInt("sleepMinute", 0)) }

    var mindfulnessEnabled by remember { mutableStateOf(settings.getBoolean("mindfulnessEnabled", true)) }
    var mindfulnessHour by remember { mutableStateOf(settings.getInt("mindfulnessHour", 9)) }
    var mindfulnessMinute by remember { mutableStateOf(settings.getInt("mindfulnessMinute", 0)) }

    var hydrationEnabled by remember { mutableStateOf(settings.getBoolean("hydrationEnabled", true)) }
    var hydrationInterval by remember { mutableStateOf(settings.getInt("hydrationInterval", 2)) }

    // Auto-Save Effect (Debounced to prevent spamming SharedPreferences/Alarms during slider drag)
    LaunchedEffect(
        studyRemindersEnabled, studyDuration, breakAlertsEnabled, breakDuration,
        limitSocialEnabled, socialLimitMins, limitStreamingEnabled, streamingLimitMins,
        sleepReminderEnabled, sleepHour, sleepMinute,
        mindfulnessEnabled, mindfulnessHour, mindfulnessMinute,
        hydrationEnabled, hydrationInterval
    ) {
        delay(300) // 300ms debounce
        
        settings.putBoolean("study_reminders", studyRemindersEnabled)
        settings.putString("study_duration", studyDuration)
        settings.putBoolean("break_alerts", breakAlertsEnabled)
        settings.putString("break_duration", breakDuration)
        settings.putBoolean("limit_social", limitSocialEnabled)
        settings.putInt("social_limit", socialLimitMins.toInt())
        settings.putBoolean("limit_streaming", limitStreamingEnabled)
        settings.putInt("streaming_limit", streamingLimitMins.toInt())
        
        settings.putBoolean("sleepReminderEnabled", sleepReminderEnabled)
        settings.putInt("sleepHour", sleepHour)
        settings.putInt("sleepMinute", sleepMinute)
        
        settings.putBoolean("mindfulnessEnabled", mindfulnessEnabled)
        settings.putInt("mindfulnessHour", mindfulnessHour)
        settings.putInt("mindfulnessMinute", mindfulnessMinute)
        
        settings.putBoolean("hydrationEnabled", hydrationEnabled)
        settings.putInt("hydrationInterval", hydrationInterval)
        
        triggerActionPlanSync()
    }

    // Helper to format time
    fun formatTime(h: Int, m: Int): String {
        val amPm = if (h >= 12) "PM" else "AM"
        val hour12 = if (h % 12 == 0) 12 else h % 12
        return "${hour12}:${m.toString().padStart(2, '0')} $amPm"
    }

    // Generate time options (every 30 mins)
    val timeOptions = remember {
        (0..23).flatMap { h ->
            listOf(0, 30).map { m ->
                val amPm = if (h >= 12) "PM" else "AM"
                val hour12 = if (h % 12 == 0) 12 else h % 12
                "${hour12}:${m.toString().padStart(2, '0')} $amPm"
            }
        }
    }
    
    val hydrationOptions = listOf("Every 1 hour", "Every 2 hours", "Every 3 hours", "Every 4 hours")

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
                    WellnessItemWithDropdown(
                        label = "Sleep Reminder",
                        currentSelection = formatTime(sleepHour, sleepMinute),
                        options = timeOptions,
                        checked = sleepReminderEnabled,
                        icon = Icons.Default.Bedtime,
                        onToggle = { sleepReminderEnabled = it },
                        onSelect = { selectedTime ->
                            val parsed = parseTimeString(selectedTime)
                            sleepHour = parsed.first
                            sleepMinute = parsed.second
                        }
                    )
                    
                    WellnessItemWithDropdown(
                        label = "Mindfulness",
                        currentSelection = formatTime(mindfulnessHour, mindfulnessMinute),
                        options = timeOptions,
                        checked = mindfulnessEnabled,
                        icon = Icons.Default.SelfImprovement,
                        onToggle = { mindfulnessEnabled = it },
                        onSelect = { selectedTime ->
                            val parsed = parseTimeString(selectedTime)
                            mindfulnessHour = parsed.first
                            mindfulnessMinute = parsed.second
                        }
                    )
                    
                    WellnessItemWithDropdown(
                        label = "Hydration",
                        currentSelection = "Every $hydrationInterval hour" + if(hydrationInterval > 1) "s" else "",
                        options = hydrationOptions,
                        checked = hydrationEnabled,
                        icon = Icons.Default.WaterDrop,
                        onToggle = { hydrationEnabled = it },
                        onSelect = { selectedStr ->
                            val hours = selectedStr.split(" ")[1].toIntOrNull() ?: 2
                            hydrationInterval = hours
                        }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// Parses "10:30 PM" back into (Hour, Minute)
fun parseTimeString(time: String): Pair<Int, Int> {
    try {
        val parts = time.split(" ")
        val timeParts = parts[0].split(":")
        var h = timeParts[0].toInt()
        val m = timeParts[1].toInt()
        val amPm = parts[1]
        
        if (amPm == "PM" && h < 12) h += 12
        if (amPm == "AM" && h == 12) h = 0
        return Pair(h, m)
    } catch (e: Exception) {
        return Pair(12, 0) // fallback
    }
}

@Composable
fun ActionCard(title: String, icon: ImageVector, iconColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
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
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeColors.textPrimary)
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
            color = ThemeColors.background,
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
fun WellnessItemWithDropdown(
    label: String, 
    currentSelection: String, 
    options: List<String>, 
    checked: Boolean, 
    icon: ImageVector, 
    onToggle: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        
        // Text Column (Clickable to open dropdown if checked)
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = checked) { expanded = true }
                .padding(vertical = 4.dp)
        ) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentSelection, 
                    fontSize = 11.sp, 
                    color = if (checked) Color(0xFF10B981) else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                if (checked) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                }
            }
            
            // Dropdown Menu
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
        
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF10B981))
        )
    }
}
