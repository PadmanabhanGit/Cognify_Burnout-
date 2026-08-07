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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralizedActionPlanScreen(navController: NavController) {
    // IMPORTANT: ActionPlanScheduler uses "action_plan" (not "action_plan_settings")
    val settings = rememberPlatformSettings("action_plan")
    
    val emeraldGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF10B981), Color(0xFF059669))
    )

    // State management for Wellness Reminders
    var sleepReminderEnabled by remember { mutableStateOf(settings.getBoolean("sleepReminderEnabled", true)) }
    var sleepHour by remember { mutableStateOf(settings.getInt("sleepHour", 22)) }
    var sleepMinute by remember { mutableStateOf(settings.getInt("sleepMinute", 0)) }

    var mindfulnessEnabled by remember { mutableStateOf(settings.getBoolean("mindfulnessEnabled", true)) }
    var mindfulnessHour by remember { mutableStateOf(settings.getInt("mindfulnessHour", 9)) }
    var mindfulnessMinute by remember { mutableStateOf(settings.getInt("mindfulnessMinute", 0)) }

    var hydrationEnabled by remember { mutableStateOf(settings.getBoolean("hydrationEnabled", true)) }
    var hydrationInterval by remember { mutableStateOf(settings.getInt("hydrationInterval", 2)) }

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
                        text = "Set wellness reminders to stay balanced",
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
                
                // Wellness Reminder Card
                ActionCard(
                    title = "Wellness Reminder",
                    icon = Icons.Default.Spa,
                    iconColor = Color(0xFF10B981)
                ) {
                    // SLEEP REMINDER
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
                    
                    // MINDFULNESS REMINDER
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
                    
                    // HYDRATION REMINDER
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

                // Save Button
                Button(
                    onClick = {
                        // Persist correct keys exactly as ActionPlanScheduler expects them
                        settings.putBoolean("sleepReminderEnabled", sleepReminderEnabled)
                        settings.putInt("sleepHour", sleepHour)
                        settings.putInt("sleepMinute", sleepMinute)
                        
                        settings.putBoolean("mindfulnessEnabled", mindfulnessEnabled)
                        settings.putInt("mindfulnessHour", mindfulnessHour)
                        settings.putInt("mindfulnessMinute", mindfulnessMinute)
                        
                        settings.putBoolean("hydrationEnabled", hydrationEnabled)
                        settings.putInt("hydrationInterval", hydrationInterval)
                        
                        // Actually trigger the background alarms with the new times!
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
