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
import com.simats.burnouttracker.utils.AppData
import com.simats.burnouttracker.utils.isAppBlockingEnabled
import com.simats.burnouttracker.utils.openAccessibilitySettings
import com.simats.burnouttracker.utils.rememberPlatformSettings
import com.simats.burnouttracker.utils.rememberUsageStatsHelper
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
    
    // "break_alerts" / "break_duration" removed: both were written here and read
    // nowhere in the codebase. No replacement control is added — the Study
    // reminder above is the one that actually schedules an alarm.

    var limitSocialEnabled by remember { mutableStateOf(settings.getBoolean("limit_social", false)) }
    var socialLimitMins by remember { mutableStateOf(settings.getInt("social_limit", 60).toFloat()) }

    var limitStreamingEnabled by remember { mutableStateOf(settings.getBoolean("limit_streaming", false)) }
    var streamingLimitMins by remember { mutableStateOf(settings.getInt("streaming_limit", 120).toFloat()) }

    // Same keys/defaults pattern as Social and Streaming; read by
    // ActionPlanReceiver, ActionPlanScheduler and AppBlockerService.
    var limitGamingEnabled by remember { mutableStateOf(settings.getBoolean("limit_gaming", false)) }
    var gamingLimitMins by remember { mutableStateOf(settings.getInt("gaming_limit", 60).toFloat()) }

    // ── Real usage for today ────────────────────────────────────────────────
    // Read once on entry through the SAME UsageStatsHelper.fetchDailyUsage()
    // every other screen uses. No second definition of "today's usage", no
    // ticker, no wall-clock additions, no optimistic increments.
    val usageHelper = rememberUsageStatsHelper()
    var usageAvailable by remember { mutableStateOf(false) }
    var socialUsedMins by remember { mutableStateOf(0) }
    var streamingUsedMins by remember { mutableStateOf(0) }
    var gamingUsedMins by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (usageHelper.hasUsageStatsPermission()) {
            val f = usageHelper.fetchDailyUsage()
            socialUsedMins = (f.socialHours * 60f).toInt()
            streamingUsedMins = (f.streamingHours * 60f).toInt()
            gamingUsedMins = (f.gamingHours * 60f).toInt()
            usageAvailable = true
        }
    }

    // Accessibility status, re-read on entry so it reflects a grant made in
    // system settings since the last visit.
    var blockingActive by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { blockingActive = isAppBlockingEnabled() }

    // Existing burnout score only — never recalculated here. AppData.hasData is
    // set once a real prediction cycle has run, so it distinguishes "no
    // assessment yet" from a genuine low score of 0.
    val burnoutAvailable = AppData.hasData
    val burnoutScore = if (burnoutAvailable) AppData.predictedScore.toInt() else null

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
        studyRemindersEnabled, studyDuration,
        limitSocialEnabled, socialLimitMins, limitStreamingEnabled, streamingLimitMins,
        limitGamingEnabled, gamingLimitMins,
        sleepReminderEnabled, sleepHour, sleepMinute,
        mindfulnessEnabled, mindfulnessHour, mindfulnessMinute,
        hydrationEnabled, hydrationInterval
    ) {
        delay(300) // 300ms debounce

        settings.putBoolean("study_reminders", studyRemindersEnabled)
        settings.putString("study_duration", studyDuration)
        settings.putBoolean("limit_social", limitSocialEnabled)
        settings.putInt("social_limit", socialLimitMins.toInt())
        settings.putBoolean("limit_streaming", limitStreamingEnabled)
        settings.putInt("streaming_limit", streamingLimitMins.toInt())
        settings.putBoolean("limit_gaming", limitGamingEnabled)
        settings.putInt("gaming_limit", gamingLimitMins.toInt())

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
                        text = "Today's Action Plan",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )
                    Text(
                        text = "Small changes to study, screen time, and recovery can help manage burnout risk.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-20).dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Current burnout risk — existing score only, never recalculated.
                BurnoutRiskSummary(score = burnoutScore)

                // 2. Entertainment Limits
                ActionCard(
                    title = "Entertainment Limits",
                    icon = Icons.Default.Block,
                    iconColor = Color(0xFFEF4444)
                ) {
                    Text(
                        text = if (usageAvailable)
                            "Daily boundaries for entertainment apps. Usage below is measured on this device today."
                        else
                            "Daily boundaries for entertainment apps. Today's usage is unavailable — Usage Access is not granted.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LimitRow(
                        label = "Social Media",
                        icon = Icons.Default.Groups,
                        accent = Color(0xFFF43F5E),
                        enabled = limitSocialEnabled,
                        onToggle = { limitSocialEnabled = it },
                        limitMins = socialLimitMins,
                        onLimitChange = { socialLimitMins = it },
                        maxMins = 120f,
                        usedMins = socialUsedMins,
                        usageAvailable = usageAvailable,
                        blockingActive = blockingActive
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    LimitRow(
                        label = "Streaming",
                        icon = Icons.Default.Tv,
                        accent = Color(0xFF3B82F6),
                        enabled = limitStreamingEnabled,
                        onToggle = { limitStreamingEnabled = it },
                        limitMins = streamingLimitMins,
                        onLimitChange = { streamingLimitMins = it },
                        maxMins = 180f,
                        usedMins = streamingUsedMins,
                        usageAvailable = usageAvailable,
                        blockingActive = blockingActive
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    LimitRow(
                        label = "Gaming",
                        icon = Icons.Default.SportsEsports,
                        accent = Color(0xFFF59E0B),
                        enabled = limitGamingEnabled,
                        onToggle = { limitGamingEnabled = it },
                        limitMins = gamingLimitMins,
                        onLimitChange = { gamingLimitMins = it },
                        maxMins = 180f,
                        usedMins = gamingUsedMins,
                        usageAvailable = usageAvailable,
                        blockingActive = blockingActive
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = ThemeColors.background, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    AppBlockingStatusRow(active = blockingActive)
                }

                // 3. Study reminders — the one notification control that is
                // actually read (StudyTrackerScreen schedules an exact alarm from
                // these two keys). Break controls removed: nothing read them.
                ActionCard(
                    title = "Study Reminders",
                    icon = Icons.Default.Timer,
                    iconColor = Color(0xFF6366F1)
                ) {
                    Text(
                        text = "Get a notification when a focus session ends, so breaks don't get skipped.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    SettingToggleRow(
                        label = "Enable Study Session Reminders",
                        checked = studyRemindersEnabled,
                        onCheckedChange = { studyRemindersEnabled = it }
                    )
                    if (studyRemindersEnabled) {
                        DurationPicker("STUDY DURATION", studyDuration, listOf("25 min", "45 min", "60 min", "90 min")) { studyDuration = it }
                    }
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

                // 5. Why this matters — descriptive only, no numeric claim about
                // the effect of any change on the burnout score.
                ActionCard(
                    title = "Why this matters",
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFF6366F1)
                ) {
                    Text(
                        text = "Entertainment usage, study time and sleep are among the activity patterns " +
                            "BurnOutTracker monitors when assessing your wellbeing. These limits and reminders " +
                            "are the actions you can take; they don't change how your risk is measured.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/** Existing burnout score, displayed as-is. Never recalculated on this screen. */
@Composable
private fun BurnoutRiskSummary(score: Int?) {
    val accent = when {
        score == null -> Color(0xFF9CA3AF)
        score > 75 -> Color(0xFFEF4444)
        score > 40 -> Color(0xFFF97316)
        else -> Color(0xFF10B981)
    }
    val label = when {
        score == null -> "Risk assessment unavailable"
        score > 75 -> "High"
        score > 40 -> "Moderate"
        else -> "Low"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Your current burnout risk", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (score == null) "--" else "$score / 100",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (score == null) Color(0xFF9CA3AF) else ThemeColors.textPrimary
                )
                Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent)
            }
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/**
 * One entertainment boundary: real usage vs the stored limit, a slider, and an
 * honest statement of what happens at the limit.
 *
 * `usedMins` comes from UsageStatsHelper.fetchDailyUsage() in the caller — the
 * same pipeline every other screen uses. Nothing here increments over time.
 */
@Composable
private fun LimitRow(
    label: String,
    icon: ImageVector,
    accent: Color,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    limitMins: Float,
    onLimitChange: (Float) -> Unit,
    maxMins: Float,
    usedMins: Int,
    usageAvailable: Boolean,
    blockingActive: Boolean
) {
    val limit = limitMins.toInt()
    val remaining = (limit - usedMins).coerceAtLeast(0)
    // Thresholds mirror the receiver's rule: it notifies only once usage has
    // passed the limit, so "Limit reached" here means the same thing there.
    val statusText: String
    val statusColor: Color
    when {
        !usageAvailable -> { statusText = "Usage unavailable"; statusColor = Color(0xFF9CA3AF) }
        usedMins > limit -> { statusText = "Limit reached"; statusColor = Color(0xFFEF4444) }
        limit > 0 && usedMins >= (limit * 0.8f) -> { statusText = "Near limit · $remaining min left"; statusColor = Color(0xFFF59E0B) }
        else -> { statusText = "Within limit · $remaining min left"; statusColor = Color(0xFF10B981) }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
                    Text(
                        text = if (!usageAvailable) "-- / $limit min today"
                               else "$usedMins min / $limit min today",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = accent)
            )
        }

        if (enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            if (usageAvailable && limit > 0) {
                LinearProgressIndicator(
                    progress = { (usedMins.toFloat() / limit.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = statusColor,
                    trackColor = ThemeColors.background
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(text = statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)

            Slider(
                value = limitMins,
                onValueChange = onLimitChange,
                valueRange = 0f..maxMins,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0m", fontSize = 10.sp, color = Color.Gray)
                Text("Daily boundary: $limit min", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text("${maxMins.toInt()}m", fontSize = 10.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(6.dp))
            // Honest about enforcement: blocking is only claimed when the
            // AccessibilityService is actually enabled on this device.
            Text(
                text = if (blockingActive)
                    "At the limit, BurnOutTracker alerts you and blocks these apps."
                else
                    "At the limit, BurnOutTracker will alert you. App blocking requires Accessibility access.",
                fontSize = 10.sp,
                color = Color.Gray,
                lineHeight = 14.sp
            )
        }
    }
}

/** Live Accessibility status for the existing AppBlockerService. */
@Composable
private fun AppBlockingStatusRow(active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("App blocking", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
            Text(
                text = if (active) "Active" else "Not enabled",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) Color(0xFF10B981) else Color(0xFF9CA3AF)
            )
        }
        if (!active) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF6366F1).copy(alpha = 0.1f),
                modifier = Modifier.clickable { openAccessibilitySettings() }
            ) {
                Text(
                    text = "Enable",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1)
                )
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
