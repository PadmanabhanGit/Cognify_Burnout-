package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.data.api.ApiClient
import com.simats.burnouttracker.data.models.SleepMoodLogRequest
import com.simats.burnouttracker.utils.rememberPlatformSettings
import kotlinx.coroutines.launch

@Composable
fun SleepMoodScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val settings = rememberPlatformSettings()
    
    var sleepHours by remember { mutableFloatStateOf(if (com.simats.burnouttracker.utils.AppData.lastSleepLogged > 0) com.simats.burnouttracker.utils.AppData.lastSleepLogged else 7.5f) }
    var selectedMood by remember { mutableIntStateOf(1) }
    var isSaving by remember { mutableStateOf(false) }

    val moodNames = listOf("Happy", "Calm", "Neutral", "Tired", "Sad")
    val moodEmojis = listOf("😊", "😌", "😐", "😴", "😢")
    val moodStatus = listOf("Excellent", "Good", "Neutral", "Tired", "Poor")
    val statusColors = listOf(Color(0xFF22C55E), Color(0xFF3B82F6), ThemeColors.textSecondary, Color(0xFFF59E0B), Color(0xFFEF4444))

    val saveLog = {
        scope.launch {
            isSaving = true
            // 1. Update In-Memory State
            com.simats.burnouttracker.utils.AppData.lastSleepLogged = sleepHours
            com.simats.burnouttracker.utils.AppData.lastMoodLogged = moodNames[selectedMood]
            
            val newLog = com.simats.burnouttracker.utils.SleepLog(
                date = "Feb 25", 
                hours = sleepHours,
                moodEmoji = moodEmojis[selectedMood],
                status = moodStatus[selectedMood],
                statusColor = statusColors[selectedMood]
            )
            com.simats.burnouttracker.utils.AppData.sleepLogs.add(0, newLog)
            com.simats.burnouttracker.utils.AppData.sleepTrendPoints.add(sleepHours / 12f)
            com.simats.burnouttracker.utils.AppData.moodTrendPoints.add((4 - selectedMood) / 4f)
            
            // 2. Persist to Disk (Actual Working)
            settings.putString("last_mood", moodNames[selectedMood])
            settings.putString("last_sleep_hours", sleepHours.toString())

            // Keep the web dashboard and the mobile app on the same Firestore-backed data.
            // Local state remains available if this request cannot be completed offline.
            try {
                ApiClient.saveSleepMoodLog(
                    SleepMoodLogRequest(
                        sleepDuration = sleepHours.toDouble(),
                        sleepQuality = ((sleepHours / 8f) * 10).toInt().coerceIn(1, 10),
                        mood = moodNames[selectedMood],
                        moodScore = (10 - (selectedMood * 2)).coerceIn(1, 10)
                    )
                )
            } catch (_: Exception) {
                // The local values above will still be retained until the user is online.
            }

            navController.navigate("sleep_mood_dashboard")
            isSaving = false
        }
    }

    val headerGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
    )

    Scaffold(
        bottomBar = { SleepMoodBottomNavigation(navController, currentRoute = "sleep_mood_logger") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColors.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 32.dp, bottom = 48.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    Surface(
                        modifier = Modifier.size(40.dp).clickable { navController.popBackStack() },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Sleep Tracker",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Track your sleep and emotional wellbeing",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset(y = (-20).dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Summary Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SleepStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Bedtime,
                        value = "${((sleepHours * 10).toInt() / 10f)}h",
                        label = "AVG SLEEP",
                        iconBgColor = Color(0xFFEEF2FF),
                        iconTint = Color(0xFF4F46E5)
                    )
                    SleepStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WbSunny,
                        value = "84%",
                        label = "SLEEP QUALITY",
                        iconBgColor = Color(0xFFFEFCE8),
                        iconTint = Color(0xFFEAB308)
                    )
                }

                // Log Sleep Hours Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Log Sleep Hours",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeColors.textPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Auto-detected", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = ((sleepHours * 10).toInt() / 10f).toString(),
                                fontSize = 56.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4F46E5)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "hours",
                                fontSize = 18.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Slider(
                            value = sleepHours,
                            onValueChange = { sleepHours = it },
                            valueRange = 0f..12f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4F46E5),
                                activeTrackColor = Color(0xFF4F46E5),
                                inactiveTrackColor = ThemeColors.border
                            )
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("0h", fontSize = 10.sp, color = Color.Gray)
                            Text("6h", fontSize = 10.sp, color = Color.Gray)
                            Text("12h", fontSize = 10.sp, color = Color.Gray)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Assessment Box
                        Surface(
                            color = Color(0xFFF5F3FF),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "ASSESSMENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                                    Text(text = if (sleepHours > 7) "Excellent Quality" else "Needs Improvement", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E1B4B))
                                }
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                // Morning Mood Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "How Are You Feeling?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeColors.textPrimary
                            )
                            Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MoodItem("Happy", "😊", selectedMood == 0) { selectedMood = 0 }
                            MoodItem("Calm", "😌", selectedMood == 1) { selectedMood = 1 }
                            MoodItem("Neutral", "😐", selectedMood == 2) { selectedMood = 2 }
                            MoodItem("Stressed", "😰", selectedMood == 3) { selectedMood = 3 }
                            MoodItem("Sad", "😢", selectedMood == 4) { selectedMood = 4 }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { saveLog() },
                            enabled = !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(text = "Save Entry", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
