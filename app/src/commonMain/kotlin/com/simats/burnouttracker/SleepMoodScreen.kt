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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.data.api.ApiClient
import com.simats.burnouttracker.data.models.SleepMoodLogRequest
import com.simats.burnouttracker.data.rememberSleepRepository
import com.simats.burnouttracker.ui.SleepViewModel
import com.simats.burnouttracker.utils.SleepLog
import com.simats.burnouttracker.utils.formatMinutes
import com.simats.burnouttracker.utils.formatTimestamp
import com.simats.burnouttracker.utils.getCurrentHourMinute
import com.simats.burnouttracker.utils.getLocalDateString
import kotlinx.coroutines.launch

/**
 * Three real states for last night's automatic sleep detection. Never a
 * fourth "fabricated" state — if none of these apply we don't render numbers.
 */
private enum class SleepDetectionState { WAITING, NO_SESSION, DETECTED }

@Composable
fun SleepMoodScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val repository = rememberSleepRepository()
    val viewModel = remember { SleepViewModel(repository) }
    val sessions by viewModel.sessions.collectAsState()

    // Same repository call SleepMoodDashboardScreen already uses to pick up
    // anything the engine analyzed since the app was last opened. Does not
    // touch SleepMonitoringEngine/SleepWorker — it only asks the existing
    // repository to re-run its existing refresh, guarded by the engine's own
    // duplicate-date check.
    LaunchedEffect(Unit) { viewModel.refreshData() }

    val today = remember { getLocalDateString() }
    val lastNightSession = sessions.firstOrNull { it.date == today }

    // Whether the automatic analysis window (closes 09:00, SleepWorker runs
    // ~09:15 — see SleepWorker.kt, unmodified) could plausibly have completed
    // yet today. This is the best available signal without a persisted
    // "analysis ran and found nothing" marker (a real gap noted in the Sleep &
    // Mood audit) — it is derived only from the current clock, never used to
    // invent a sleep value.
    val (hour, minute) = remember { getCurrentHourMinute() }
    val analysisWindowLikelyClosed = hour > 9 || (hour == 9 && minute >= 15)

    val sleepState = when {
        lastNightSession != null -> SleepDetectionState.DETECTED
        !analysisWindowLikelyClosed -> SleepDetectionState.WAITING
        else -> SleepDetectionState.NO_SESSION
    }

    var showManualFallback by remember { mutableStateOf(false) }

    // Mood names and the buttons below must always match 1:1 — a prior bug
    // saved "Tired" while the button visibly read "Stressed" for the same
    // index. Fixed here since this whole screen is being rewritten.
    val moodNames = listOf("Happy", "Calm", "Neutral", "Stressed", "Sad")
    val moodEmojis = listOf("😊", "😌", "😐", "😰", "😢")
    val moodStatus = listOf("Excellent", "Good", "Neutral", "Stressed", "Poor")
    val statusColors = listOf(Color(0xFF22C55E), Color(0xFF3B82F6), ThemeColors.textSecondary, Color(0xFFF59E0B), Color(0xFFEF4444))
    var selectedMood by remember { mutableStateOf<Int?>(null) }
    var isSavingMood by remember { mutableStateOf(false) }

    var manualHours by remember { mutableFloatStateOf(7.5f) }
    var isSavingManualSleep by remember { mutableStateOf(false) }

    /**
     * Today's MANUAL entry for this account, read back from the backend.
     *
     * Manual sleep was write-only. It POSTs to /api/sleep-mood/log and reaches
     * Firestore, but nothing ever read it back: Room holds detected sessions only
     * (SleepRestorePlanner rejects `source != "automatic"`), and AppData is
     * in-memory and cleared on every account change. So the value vanished the
     * moment the app restarted or the account switched, and the screen asked for
     * it again as though it had never been entered.
     *
     * Fetched per account by the server's own uid filter — there is no uid
     * parameter here to get wrong.
     */
    var savedManualSleepHours by remember { mutableStateOf<Float?>(null) }
    var manualSleepLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val response = ApiClient.getSleepMoodLogs(60)
            if (response.success) {
                val todayManual = response.logs.firstOrNull {
                    it.date == today && it.source == "manual" && (it.sleepDuration ?: 0.0) > 0.0
                }
                todayManual?.sleepDuration?.let { hours ->
                    savedManualSleepHours = hours.toFloat()
                    manualHours = hours.toFloat()
                    com.simats.burnouttracker.utils.AppData.lastSleepLogged = hours.toFloat()
                }
            }
        } catch (e: Exception) {
            // Non-fatal: the screen still works, it just cannot pre-fill.
            println("[SLEEP MANUAL] could not load today's manual entry: ${e::class.simpleName}")
        }
        manualSleepLoaded = true
    }

    val saveMood: () -> Unit = {
        val moodIndex = selectedMood
        if (moodIndex != null) {
            scope.launch {
                isSavingMood = true
                com.simats.burnouttracker.utils.AppData.lastMoodLogged = moodNames[moodIndex]
                try {
                    ApiClient.saveSleepMoodLog(
                        SleepMoodLogRequest(
                            sleepDuration = null,
                            sleepQuality = null,
                            mood = moodNames[moodIndex],
                            moodScore = (10 - (moodIndex * 2)).coerceIn(1, 10),
                            source = "manual",
                            // Same reason as the manual-sleep save below: the
                            // device's local date, not the server's.
                            date = today
                        )
                    )
                } catch (_: Exception) {
                    // AppData.lastMoodLogged above still reflects the user's choice locally.
                }
                isSavingMood = false
            }
        }
    }

    val saveManualSleep: () -> Unit = {
        val moodIndex = selectedMood ?: 2 // "Neutral" default — sleep can be logged without picking a mood first.
        scope.launch {
            isSavingManualSleep = true
            val quality1to10 = ((manualHours / 8f) * 10).toInt().coerceIn(1, 10)
            com.simats.burnouttracker.utils.AppData.lastSleepLogged = manualHours
            // Reflect it immediately AND keep it after the screen is rebuilt: the
            // LaunchedEffect above re-reads this from the backend on next open.
            savedManualSleepHours = manualHours
            com.simats.burnouttracker.utils.AppData.sleepLogs.add(
                0,
                SleepLog(
                    date = today,
                    hours = manualHours,
                    moodEmoji = moodEmojis[moodIndex],
                    status = moodStatus[moodIndex],
                    statusColor = statusColors[moodIndex]
                )
            )
            try {
                ApiClient.saveSleepMoodLog(
                    SleepMoodLogRequest(
                        sleepDuration = manualHours.toDouble(),
                        sleepQuality = quality1to10,
                        mood = moodNames[moodIndex],
                        moodScore = (10 - (moodIndex * 2)).coerceIn(1, 10),
                        source = "manual",
                        // Send the DEVICE's local date, as the automatic path
                        // already does. Omitting it made the server fall back to
                        // normalizeDateValue(new Date()), which is server-local —
                        // and Render runs in UTC while every read side of this app
                        // keys on IST. An entry saved between 00:00 and 05:30 IST
                        // was therefore filed under the previous day and could
                        // never match the dashboard's "today".
                        date = today
                    )
                )
            } catch (_: Exception) {
                // Local state above already reflects the manual entry.
            }
            isSavingManualSleep = false
            showManualFallback = false
        }
    }

    val headerGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
    )

    Scaffold(
        bottomBar = { SleepMoodBottomNavigation(navController, currentRoute = "sleep_mood") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColors.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
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
                        text = "Sleep & Mood",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Automatically detected sleep, and how you're feeling",
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
                // ── PRIMARY: Last Night's Sleep (automatic detection) ────────────
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
                                text = "Last Night's Sleep",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeColors.textPrimary
                            )
                            Icon(Icons.Default.Bedtime, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        when (sleepState) {
                            SleepDetectionState.DETECTED -> {
                                val session = lastNightSession!!
                                DetectedSleepSummary(
                                    sleepQuality = session.sleepQuality,
                                    totalSleepMinutes = session.totalSleepMinutes,
                                    sleepStart = session.sleepStart,
                                    sleepEnd = session.sleepEnd,
                                    awakeningCount = session.awakeningCount,
                                    disturbanceScore = session.disturbanceScore
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { navController.navigate("sleep_mood_dashboard") },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                ) {
                                    Text("View Sleep Analysis", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            SleepDetectionState.WAITING -> {
                                EmptyStateMessage(
                                    icon = Icons.Default.HourglassEmpty,
                                    title = "Sleep analysis isn't ready yet",
                                    body = "Your sleep is automatically analyzed after the monitoring window closes, usually mid-morning."
                                )
                            }
                            SleepDetectionState.NO_SESSION -> {
                                EmptyStateMessage(
                                    icon = Icons.Default.NightlightRound,
                                    title = "No clear sleep session detected",
                                    body = "We couldn't find a clear enough inactivity-then-activity pattern last night to confidently detect sleep. This can happen on nights with unusual phone usage."
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Show what was already logged rather than asking
                                // again. Previously this always read "Log it
                                // manually", because the entered value was never
                                // read back — so a user who had already logged
                                // their night was invited to log it a second time.
                                val alreadyLogged = savedManualSleepHours
                                if (alreadyLogged != null) {
                                    Text(
                                        text = "You logged ${((alreadyLogged * 10).toInt() / 10f)}h manually for today.",
                                        color = Color(0xFF059669),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Text(
                                    text = if (alreadyLogged != null) "Update it" else "Didn't detect your sleep? Log it manually.",
                                    color = Color(0xFF6366F1),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { showManualFallback = !showManualFallback }
                                )
                                if (showManualFallback) {
                                    Column(modifier = Modifier.padding(top = 16.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = ((manualHours * 10).toInt() / 10f).toString(),
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF4F46E5)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "hours", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                                        }
                                        Slider(
                                            value = manualHours,
                                            onValueChange = { manualHours = it },
                                            valueRange = 0f..12f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFF4F46E5),
                                                activeTrackColor = Color(0xFF4F46E5),
                                                inactiveTrackColor = ThemeColors.border
                                            )
                                        )
                                        Button(
                                            onClick = saveManualSleep,
                                            enabled = !isSavingManualSleep,
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                        ) {
                                            if (isSavingManualSleep) {
                                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                            } else {
                                                Text("Save Manual Entry", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── SECONDARY: Mood (manual, always available) ───────────────────
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
                                text = "How Are You Feeling Today?",
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
                            moodNames.forEachIndexed { index, name ->
                                MoodItem(name, moodEmojis[index], selectedMood == index) { selectedMood = index }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = saveMood,
                            enabled = !isSavingMood && selectedMood != null,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            if (isSavingMood) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                            } else {
                                Text(text = "Save Mood", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DetectedSleepSummary(
    sleepQuality: Int,
    totalSleepMinutes: Int,
    sleepStart: Long,
    sleepEnd: Long,
    awakeningCount: Int,
    disturbanceScore: Int
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(text = "$sleepQuality%", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = qualityColor(sleepQuality))
            Text(text = qualityLevel(sleepQuality), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = qualityColor(sleepQuality))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = formatMinutes(totalSleepMinutes), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
            Text(text = "Total Sleep", fontSize = 11.sp, color = Color.Gray)
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SleepStatCard(modifier = Modifier.weight(1f), icon = Icons.Default.Nightlight, value = formatTimestamp(sleepStart), label = "SLEEP START", iconBgColor = Color(0xFFEEF2FF), iconTint = Color(0xFF4F46E5))
        Spacer(modifier = Modifier.width(12.dp))
        SleepStatCard(modifier = Modifier.weight(1f), icon = Icons.Default.WbSunny, value = formatTimestamp(sleepEnd), label = "WAKE UP", iconBgColor = Color(0xFFFEFCE8), iconTint = Color(0xFFEAB308))
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SleepStatCard(modifier = Modifier.weight(1f), icon = Icons.Default.NotificationsActive, value = "$awakeningCount", label = "AWAKENINGS", iconBgColor = Color(0xFFFEF2F2), iconTint = Color(0xFFEF4444))
        Spacer(modifier = Modifier.width(12.dp))
        SleepStatCard(modifier = Modifier.weight(1f), icon = Icons.Default.Warning, value = "$disturbanceScore", label = "DISTURBANCE", iconBgColor = Color(0xFFFEF2F2), iconTint = Color(0xFFEF4444))
    }
}

@Composable
private fun EmptyStateMessage(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Icon(icon, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = body, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 16.sp)
    }
}

private fun qualityLevel(score: Int): String = when {
    score >= 90 -> "Excellent"
    score >= 75 -> "Good"
    score >= 60 -> "Moderate"
    score >= 40 -> "Poor"
    else -> "Very Poor"
}

private fun qualityColor(score: Int): Color = when {
    score >= 75 -> Color(0xFF10B981)
    score >= 60 -> Color(0xFFF59E0B)
    else -> Color(0xFFEF4444)
}
