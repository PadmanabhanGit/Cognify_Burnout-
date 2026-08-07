package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.data.api.ApiClient
import com.simats.burnouttracker.data.models.StartSessionRequest
import com.simats.burnouttracker.utils.AppData
import com.simats.burnouttracker.utils.rememberPlatformSettings
import com.simats.burnouttracker.utils.rememberTimerHelper
import com.simats.burnouttracker.utils.getCurrentTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.simats.burnouttracker.data.models.OfflineSessionRequest

@Composable
fun StudyTrackerScreen(navController: NavController) {
    val settings = rememberPlatformSettings("study_tracker")
    val actionPlanSettings = rememberPlatformSettings("action_plan")
    var isTimerRunning by remember { mutableStateOf(AppData.activeSessionName != null) }
    var elapsedTimeSeconds by remember { mutableLongStateOf(0L) }
    var showSessionPrompt by remember { mutableStateOf(false) }
    var sessionNameInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    val timerHelper = rememberTimerHelper()

    LaunchedEffect(Unit) {
        // 1. Immediately load local cache to prevent empty states on redeploys
        AppData.studyTodayHours = settings.getString("studyTodayHours", "0.0")?.toFloatOrNull() ?: 0f
        AppData.studyWeekHours = settings.getString("studyWeekHours", "0.0")?.toFloatOrNull() ?: 0f
        
        // 2. Fetch from backend to sync across platforms
        try {
            val response = ApiClient.getStudyWeeklyStats()
            if (response.success && response.stats != null) {
                val stats = response.stats
                
                // Only overwrite if backend has more or equal data (prevents overwriting local unsynced data)
                val backendWeek = stats.totalHours.toFloat()
                if (backendWeek >= AppData.studyWeekHours) {
                    AppData.studyWeekHours = backendWeek
                    settings.putString("studyWeekHours", backendWeek.toString())
                }
                
                val todayStr = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
                val todayMins = stats.dailyTotals[todayStr] ?: 0
                val backendToday = (todayMins / 60f)
                
                if (backendToday >= AppData.studyTodayHours) {
                    AppData.studyTodayHours = backendToday
                    settings.putString("studyTodayHours", backendToday.toString())
                }
                
                AppData.weeklyStudyData.clear()
                for (i in 0..6) AppData.weeklyStudyData.add(0f) 
                
                stats.subjectBreakdown?.let { breakdown ->
                    breakdown.forEach { (subject, mins) ->
                        AppData.studyBreakdown[subject] = (mins / 60f)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Process Offline Queue
        scope.launch {
            try {
                val pendingJson = settings.getString("pending_sessions", "[]") ?: "[]"
                val pendingQueue = try {
                    Json.decodeFromString<List<OfflineSessionRequest>>(pendingJson).toMutableList()
                } catch (e: Exception) {
                    mutableListOf<OfflineSessionRequest>()
                }
                
                if (pendingQueue.isNotEmpty()) {
                    val syncedIndices = mutableListOf<Int>()
                    for ((index, request) in pendingQueue.withIndex()) {
                        val resp = ApiClient.logOfflineSession(request)
                        if (resp.success) {
                            syncedIndices.add(index)
                        }
                    }
                    
                    // Remove synced and save remaining
                    syncedIndices.sortedDescending().forEach { pendingQueue.removeAt(it) }
                    settings.putString("pending_sessions", Json.encodeToString(pendingQueue))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(isTimerRunning, AppData.sessionStartTime) {
        while (isTimerRunning) {
            val start = AppData.sessionStartTime
            if (start != null) {
                elapsedTimeSeconds = (getCurrentTimeMillis() - start) / 1000
            }
            delay(1000)
        }
    }

    if (showSessionPrompt) {
        AlertDialog(
            onDismissRequest = { showSessionPrompt = false },
            title = { Text("What are you working on?") },
            text = {
                OutlinedTextField(
                    value = sessionNameInput,
                    onValueChange = { sessionNameInput = it },
                    placeholder = { Text("e.g. Mathematics, Project Research") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = sessionNameInput.isNotBlank(),
                    onClick = {
                        val name = sessionNameInput
                        AppData.activeSessionName = name
                        AppData.sessionStartTime = getCurrentTimeMillis()
                        isTimerRunning = true
                        showSessionPrompt = false
                        timerHelper.startTimer(name)
                        
                        // Start session on backend
                        scope.launch {
                            try {
                                val response = ApiClient.startStudySession(StartSessionRequest(subject = name))
                                if (response.success && response.session != null) {
                                    AppData.activeSessionId = response.session.id
                                }
                            } catch (e: Exception) {}
                        }

                        // Schedule local alarm if reminders are enabled
                        if (actionPlanSettings.getBoolean("study_reminders", true)) {
                            val durationStr = actionPlanSettings.getString("study_duration", "45 min") ?: "45 min"
                            val mins = durationStr.replace(" min", "").trim().toIntOrNull() ?: 45
                            com.simats.burnouttracker.utils.scheduleStudyTimer(mins)
                        }
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSessionPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val headerGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
    )

    Scaffold(
        containerColor = ThemeColors.background,
        bottomBar = { AppBottomNavigation(navController, "tracker") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section (STT Image)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(headerGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 40.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Study Time Tracking",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Monitor your study sessions and analytics",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset(y = (-30).dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Current Session Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Current Session", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeColors.textPrimary)
                            }
                            Surface(
                                color = if (isTimerRunning) Color(0xFFFEF08A) else Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isTimerRunning) "IN PROGRESS" else "READY",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTimerRunning) Color(0xFFA16207) else Color(0xFF16A34A)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        val displayTime = formatElapsedTime(elapsedTimeSeconds)
                        Text(
                            text = displayTime,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                if (isTimerRunning) {
                                    timerHelper.stopTimer()
                                    isTimerRunning = false
                                    com.simats.burnouttracker.utils.cancelStudyTimer()
                                    val hours = elapsedTimeSeconds / 3600f
                                    val minutes = (elapsedTimeSeconds / 60).toInt()
                                    val sessionName = AppData.activeSessionName ?: "Unknown"
                                    val sessionStartIso = AppData.sessionStartTime?.let { 
                                        Instant.fromEpochMilliseconds(it).toString() 
                                    } ?: Clock.System.now().toString()

                                    // Stop session on backend or queue for offline sync
                                    scope.launch {
                                        var syncSuccess = false
                                        try {
                                            val sessionId = AppData.activeSessionId
                                            if (sessionId != null) {
                                                val resp = ApiClient.stopStudySession(sessionId)
                                                syncSuccess = resp.success
                                            }
                                        } catch (e: Exception) {}

                                        if (!syncSuccess) {
                                            // Queue offline
                                            try {
                                                val pendingJson = settings.getString("pending_sessions", "[]") ?: "[]"
                                                val pendingQueue = try {
                                                    Json.decodeFromString<List<OfflineSessionRequest>>(pendingJson).toMutableList()
                                                } catch (e: Exception) {
                                                    mutableListOf<OfflineSessionRequest>()
                                                }
                                                pendingQueue.add(OfflineSessionRequest(subject = sessionName, duration = minutes, startTime = sessionStartIso))
                                                settings.putString("pending_sessions", Json.encodeToString(pendingQueue))
                                            } catch (e: Exception) {}
                                        }
                                    }

                                    AppData.studyTodayHours += hours
                                    AppData.studyWeekHours += hours
                                    AppData.studyMonthHours += hours
                                    AppData.studyBreakdown[sessionName] = (AppData.studyBreakdown[sessionName] ?: 0f) + hours
                                    
                                    // Save robustly to local cache
                                    settings.putString("studyTodayHours", AppData.studyTodayHours.toString())
                                    settings.putString("studyWeekHours", AppData.studyWeekHours.toString())
                                    
                                    // Update BurnoutFeatures for prediction
                                    AppData.currentFeatures = AppData.currentFeatures.copy(
                                        productivityHours = AppData.currentFeatures.productivityHours + hours,
                                        totalScreenTime = AppData.currentFeatures.totalScreenTime + hours
                                    )
                                    
                                    // Update graph data (assuming Mon-Sun index)
                                    val currentDay = 0 // Mock day index
                                    AppData.weeklyStudyData[currentDay] += hours
                                    
                                    // Update monthly trend (Week 4)
                                    val currentWeekIndex = 3
                                    val newTrendValue = (AppData.studyWeekHours / 40f).coerceIn(0f, 1f)
                                    AppData.monthlyStudyTrend[currentWeekIndex] = newTrendValue
                                    
                                    AppData.activeSessionName = null
                                    AppData.activeSessionId = null
                                    AppData.sessionStartTime = null
                                    elapsedTimeSeconds = 0
                                    sessionNameInput = ""
                                } else {
                                    showSessionPrompt = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isTimerRunning) Color(0xFFEF4444) else Color(0xFF2563EB))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (isTimerRunning) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = if (isTimerRunning) "End Session" else "Start Session", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val currentAddedSeconds = if (isTimerRunning) elapsedTimeSeconds else 0L
                        val todaysDisplay = getFormattedExactDuration(AppData.studyTodayHours, currentAddedSeconds)
                        val weeklyDisplay = getFormattedExactDuration(AppData.studyWeekHours, currentAddedSeconds)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SessionStatItem(label = "Today's Total", value = todaysDisplay, color = Color(0xFFEFF6FF), textColor = Color(0xFF2563EB), modifier = Modifier.weight(1f))
                            SessionStatItem(label = "This Week", value = weeklyDisplay, color = Color(0xFFFAF5FF), textColor = Color(0xFF9333EA), modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 2. Weekly Overview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Weekly Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeColors.textPrimary)
                            }
                            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Bar Chart
                        Row(modifier = Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            val maxHours = AppData.weeklyStudyData.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                            AppData.weeklyStudyData.forEachIndexed { index, value ->
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                    Box(modifier = Modifier.fillMaxWidth().height(((value / maxHours) * 120).dp).background(Color(0xFF3B82F6), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = days[index], fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { navController.navigate("study_tracker_details") },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.card, contentColor = Color(0xFF9333EA)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ThemeColors.background)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "View Detailed Trends", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun SessionStatItem(label: String, value: String, color: Color, textColor: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = color) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 12.sp, color = textColor.copy(alpha = 0.7f))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }
    }
}

private fun formatElapsedTime(seconds: Long): String {
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

private fun getFormattedExactDuration(hoursDecimal: Float, addedSeconds: Long): String {
    val totalSecs = (hoursDecimal * 3600).toLong() + addedSeconds
    if (totalSecs <= 0) return "0s"
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    
    val parts = mutableListOf<String>()
    if (h > 0) parts.add("${h}h")
    if (m > 0) parts.add("${m}m")
    if (s > 0) parts.add("${s}s")
    
    return if (parts.isNotEmpty()) parts.joinToString(" ") else "0s"
}
