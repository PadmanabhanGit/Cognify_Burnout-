package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavController
import com.simats.burnouttracker.data.api.ApiClient
import com.simats.burnouttracker.data.rememberSleepRepository
import com.simats.burnouttracker.utils.*
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(navController: NavController) {
    val settings = rememberPlatformSettings()
    val studySettings = rememberPlatformSettings("study_tracker")
    val firstName = AppData.userFullName?.split(" ")?.firstOrNull() ?: settings.getString("firstName", "Student") ?: "Student"
    
    val predictor = rememberBurnoutPredictor()
    val usageHelper = rememberUsageStatsHelper()
    val sleepRepository = rememberSleepRepository()
    val sleepSessions by sleepRepository.getRecentSessions().collectAsState(emptyList())
    val latestSleepSession = sleepSessions.firstOrNull()

    // ── Dashboard sleep summary: TODAY ONLY ──────────────────────────────────
    // `latestSleepSession` above is the newest row of ANY date. Using it for the
    // sleep summary meant that at 06:47 on Aug 11 — before today's night has
    // been analysed — the Dashboard reported a previous day's session ("4.9H")
    // while Sleep & Mood correctly said "Sleep analysis isn't ready yet". Two
    // screens, two answers, from two different selections of the same table.
    //
    // These use the SAME predicate SleepMoodScreen uses (`it.date == today`), so
    // the Dashboard and Sleep & Mood can no longer disagree. A missing session
    // deliberately does NOT fall back to yesterday's — that is precisely the
    // stale-data behaviour being removed.
    //
    // `latestSleepSession` is intentionally left in place for the
    // AppData.lastSleepLogged write below: that value feeds BurnoutPredictor and
    // ProductivityPredictor, and changing its semantics would alter burnout /
    // productivity output, which is out of scope for this display fix.
    val todayDate = remember { getLocalDateString() }
    val todaySleepSession = sleepSessions.firstOrNull { it.date == todayDate }
    val todayManualSleep = AppData.sleepLogs.firstOrNull { it.date == todayDate }

    var riskScore by remember { mutableStateOf(AppData.predictedScore) }
    var riskLevel by remember { mutableStateOf(getRiskLevelName(riskScore)) }
    var currentDate by remember { mutableStateOf(formatDashboardDate()) }
    var activeTimerSeconds by remember { mutableLongStateOf(0L) }

    // True once GET /api/dashboard has confirmed a canonical persisted
    // productivity score for today (productivityLogs/{userId}_{IST-date} —
    // the same document ProductivityScreen/GET /api/productivity/today read).
    // While false, the local ProductivityPredictor may still compute a
    // candidate (it's still useful before a canonical record exists for
    // today), but once true, that candidate must never overwrite the real
    // persisted value again this session.
    var isProductivityScoreCanonical by remember { mutableStateOf(false) }
    
    LaunchedEffect(AppData.activeSessionName, AppData.sessionStartTime) {
        while(AppData.activeSessionName != null) {
            val start = AppData.sessionStartTime
            if (start != null) {
                activeTimerSeconds = (getCurrentTimeMillis() - start) / 1000
            }
            delay(1000)
        }
        activeTimerSeconds = 0L
    }
    
    // Run the existing sleep analysis once when the Dashboard opens.
    //
    // refreshSleepData() was previously only ever called from the three Sleep &
    // Mood screens, so after the early-finalization change the engine could
    // confirm a wake at ~06:01 but nothing would actually invoke it until the
    // user navigated into Sleep & Mood — the Dashboard card would still read
    // "Not analyzed yet" on app open. The daily 09:15 SleepWorker is also not a
    // dependable early path: SleepWorker.doWork() returns immediately unless
    // both AppData.allowAllNotif and AppData.studyPrompts are true.
    //
    // This is the existing mechanism, invoked once per screen entry — not
    // polling. AndroidSleepRepository's single-flight guard collapses concurrent
    // callers, and the engine's duplicate-date guard makes an already-analysed
    // night a no-op, so this cannot produce extra work or extra rows.
    LaunchedEffect(Unit) {
        sleepRepository.refreshSleepData()
    }

    LaunchedEffect(latestSleepSession) {
        latestSleepSession?.let {
            AppData.lastSleepLogged = it.totalSleepMinutes / 60f
        }
    }
    
    LaunchedEffect(Unit) {
        // Load initial state from cache so it's not empty on app restart
        if (AppData.studyTodayHours == 0f) {
            AppData.studyTodayHours = studySettings.getString("studyTodayHours", "0.0")?.toFloatOrNull() ?: 0f
        }
        
        while(true) {
            currentDate = formatDashboardDate()
            if (usageHelper.hasUsageStatsPermission()) {
                val realFeatures = usageHelper.fetchDailyUsage()
                AppData.currentFeatures = realFeatures
                val prediction = predictor.predict(realFeatures)
                AppData.predictedScore = prediction
                riskScore = prediction
                riskLevel = getRiskLevelName(prediction)
                
                // Calculate a candidate Productivity Score. Only applied when
                // today's canonical persisted score isn't already known — once
                // the GET below confirms a real value, this local recompute
                // must not silently replace it (see isProductivityScoreCanonical).
                if (!isProductivityScoreCanonical) {
                    val newProdScore = ProductivityPredictor.calculate(realFeatures, prediction, AppData.lastSleepLogged)
                    AppData.productivityScore = newProdScore
                }
                
                // Update dynamic productivity metrics for mini-cards
                // Use a standard 8-hour goal for the goal hit rate
                AppData.goalHitRate = ((AppData.studyTodayHours / 8f) * 100f).toInt().coerceIn(0, 100)
                // Peak focus is derived from average session length (assume max session is ~2x average, converted to hours)
                AppData.peakFocusHours = ((realFeatures.averageSessionMinutes * 2f) / 60f).coerceAtLeast(0.2f)

                // Cache today's study hours locally to prevent reset on restart
                studySettings.putString("studyTodayHours", AppData.studyTodayHours.toString())
                
                // ── Persist Android's calculated burnout result ──────────────────
                // Android is authoritative. Everything below is READ from the existing
                // Android calculation (TFLite prediction → InsightGenerator →
                // WellbeingGenerator) — nothing is recalculated here, and nothing is
                // recalculated on the backend or on the Web.
                val insights = InsightGenerator.generate(realFeatures, prediction)
                val wellbeing = WellbeingGenerator.generate(prediction, insights)

                try {
                    val assessmentResponse = ApiClient.saveBurnoutAssessment(
                        com.simats.burnouttracker.data.models.BurnoutAssessmentRequest(
                            riskScore = prediction.toInt(),
                            riskLevel = riskLevel.lowercase(),
                            assessment = burnoutAssessmentText(prediction),
                            warnings = burnoutWarningIndicators(prediction),
                            factors = listOf(
                                com.simats.burnouttracker.data.models.BurnoutFactor("Study Hours", insights.studyLoad),
                                com.simats.burnouttracker.data.models.BurnoutFactor("Sleep Quality", insights.sleepQuality),
                                com.simats.burnouttracker.data.models.BurnoutFactor("Stress Level", insights.stressLevel),
                                com.simats.burnouttracker.data.models.BurnoutFactor("Recovery Time", insights.recoveryTime)
                            ),
                            wellbeing = com.simats.burnouttracker.data.models.BurnoutWellbeing(
                                focus = wellbeing.focus,
                                stress = wellbeing.stress,
                                mood = wellbeing.mood,
                                energy = wellbeing.energy,
                                sleep = wellbeing.sleep,
                                study = wellbeing.studyLoad
                            ),
                            recommendations = RecommendationEngine.generate(prediction).map { it.title }
                        )
                    )
                    if (!assessmentResponse.success) {
                        println("[BURNOUT SYNC] POST /api/burnout/assessment rejected: ${assessmentResponse.message ?: "no message"}")
                    }
                } catch (e: Exception) {
                    println("[BURNOUT SYNC] POST /api/burnout/assessment failed: ${e::class.simpleName}: ${e.message}")
                    e.printStackTrace()
                }

                // Mock sleep logs removed. "Feb 24 / 8.0h / Excellent" and
                // "Feb 23 / 6.5h / Fair" were injected whenever the list was empty
                // and rendered as real entries on the Dashboard and the Sleep &
                // Mood screen. Real entries come from the user's own sleep/mood
                // logging; an empty list now stays empty.

                AppData.hasData = true
                AppData.lastUpdatedTime = formatCurrentTime()
            }
            
            val response = ApiClient.getDashboard()
            if (response.success && response.dashboard?.user?.firstName != null) {
                val fName = response.dashboard.user.firstName
                if (fName.isNotBlank() && settings.getString("firstName", "") != fName) {
                    settings.putString("firstName", fName)
                    if (AppData.userFullName.isNullOrBlank()) {
                        AppData.userFullName = fName
                    }
                }
            }
            // Canonical productivity score for today, from the same
            // productivityLogs/{userId}_{IST-date} document ProductivityScreen
            // reads via GET /api/productivity/today. When present, it becomes
            // authoritative for the rest of this session (see the predictor
            // guard above) — a null here just means no canonical record exists
            // yet; it must never be treated as "productivity is 0" and must
            // never fall back to a stale prior-day value (none is read here).
            val canonicalProductivity = response.dashboard?.quickStats?.lastProductivityScore
            if (response.success && canonicalProductivity != null) {
                AppData.productivityScore = canonicalProductivity
                isProductivityScoreCanonical = true
            }
            delay(30000) // Refresh every 30 seconds for "real-time" feel
        }
    }

    val headerGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF6B21A8), Color(0xFF3B82F6))
    )

    Scaffold(
        containerColor = ThemeColors.background,
        bottomBar = { AppBottomNavigation(navController, "dashboard") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section (Image 3 style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 40.dp, bottom = 60.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hello, $firstName!",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentDate,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                        // Corrected Profile Icon: White Circle, Purple Icon
                        Surface(
                            modifier = Modifier.size(48.dp).testTag("profileIcon"),
                            shape = CircleShape,
                            color = Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Psychology, 
                                    contentDescription = null, 
                                    tint = Color(0xFF6B21A8), 
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Summary Row
                    val todayStudyDisplay = getFormattedExactDuration(AppData.studyTodayHours, activeTimerSeconds)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            icon = Icons.Default.AccessTime,
                            value = todayStudyDisplay,
                            label = "Study Today",
                            modifier = Modifier.weight(1f)
                        )
                        // Three distinct, honest states. The previous
                        // `?: (AppData.lastSleepLogged * 60)` fallback is removed:
                        // that global is written from the newest session of any
                        // date and from manual entry, so it could surface an
                        // unrelated number under a "Sleep" label with no way to
                        // tell where it came from.
                        SummaryCard(
                            icon = Icons.Default.Bedtime,
                            value = when {
                                todaySleepSession != null ->
                                    formatDisplayTime(todaySleepSession.totalSleepMinutes * 60L)
                                todayManualSleep != null ->
                                    formatDisplayTime((todayManualSleep.hours * 3600f).toLong())
                                else -> "--"
                            },
                            label = when {
                                todaySleepSession != null -> "Sleep"
                                todayManualSleep != null -> "Sleep · Manual"
                                else -> "Not analyzed yet"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            icon = Icons.Default.SentimentSatisfiedAlt,
                            value = getMoodEmoji(AppData.lastMoodLogged),
                            label = "Mood",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset(y = (-30).dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overlapping Burnout Alert Card
                BurnoutAlertBox(
                    riskLevel = riskLevel,
                    riskScore = riskScore.toInt(),
                    onClick = { navController.navigate("burnout_risk") },
                    modifier = Modifier.testTag("burnoutAlertBox")
                )

                Text(
                    text = "Features",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ThemeColors.textPrimary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )

                // Feature List
                val totalStudyHours = AppData.studyTodayHours + (activeTimerSeconds / 3600f)
                val todayStudyDisplay = getFormattedExactDuration(AppData.studyTodayHours, activeTimerSeconds)
                FeatureCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Study Tracking",
                    subtitle = "Daily goal progress",
                    trailing = todayStudyDisplay,
                    progress = (totalStudyHours / 8f).coerceIn(0f, 1f), // Goal is 8h
                    color = Color(0xFFE0F2FE),
                    iconColor = Color(0xFF0284C7),
                    onClick = { navController.navigate("study_tracker") },
                    modifier = Modifier.testTag("featureStudyTracking")
                )

                FeatureCard(
                    icon = Icons.Default.Bedtime,
                    title = "Sleep & Mood",
                    subtitle = "Wellness analysis",
                    // Badge distinguishes automatic detection from a manual entry
                    // instead of showing a mood status from any date.
                    trailing = when {
                        todaySleepSession != null -> "${todaySleepSession.sleepQuality}%"
                        todayManualSleep != null -> "Manual"
                        else -> "Log Today"
                    },
                    // Bar is today's REAL detected quality, or absent entirely.
                    // It was previously `(latestSleepSession?.sleepQuality ?: 0f) / 100f`
                    // — a stale session's quality, and on a genuinely empty state a
                    // 0% bar that looked like a measured result rather than no data.
                    // FeatureCard already omits the bar when progress is null.
                    progress = todaySleepSession?.let { it.sleepQuality / 100f },
                    color = Color(0xFFEEF2FF),
                    iconColor = Color(0xFF6366F1),
                    onClick = { navController.navigate("sleep_mood") },
                    modifier = Modifier.testTag("featureSleepMood")
                )

                FeatureCard(
                    icon = Icons.Default.BarChart,
                    title = "App Usage",
                    subtitle = "Leisure time impact",
                    // Was: (socialHours + gamingHours + streamingHours) — entertainment
                    // only, with Productivity excluded, which is why this card read
                    // "1.9H" while the App Usage screen's own "Total App Usage" read
                    // ~4h. It now reads the SAME field that screen displays,
                    // AppData.currentFeatures.totalScreenTime
                    // (EntertainmentAppUsageScreen.kt, formatHours(features.totalScreenTime)),
                    // so there is one total, not two. No new calculation is introduced.
                    trailing = formatCompactUsage((AppData.currentFeatures.totalScreenTime * 3600).toLong()),
                    progress = (AppData.currentFeatures.totalScreenTime / 10f).coerceIn(0f, 1f),
                    color = Color(0xFFF5F3FF),
                    iconColor = Color(0xFF8B5CF6),
                    onClick = { navController.navigate("entertainment_usage") },
                    modifier = Modifier.testTag("featureAppUsage")
                )

                FeatureCard(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = "Productivity",
                    subtitle = "Weekly trends",
                    // Previously "+${(AppData.productivityScore % 15) + 5}%" — a
                    // fabricated week-over-week change with no real weekly-history
                    // source behind it (no such data exists on Android or the
                    // backend). Shows the real canonical score when it's known;
                    // matches Web Dashboard.jsx's own fallback text ('View') for
                    // the same "not confirmed yet" case rather than showing an
                    // unconfirmed local predictor candidate as if it were the
                    // persisted score.
                    trailing = if (isProductivityScoreCanonical) "${AppData.productivityScore}%" else "View",
                    color = Color(0xFFDCFCE7),
                    iconColor = Color(0xFF10B981),
                    onClick = { navController.navigate("productivity") },
                    modifier = Modifier.testTag("featureProductivity")
                )

                FeatureCard(
                    icon = Icons.Default.Description,
                    title = "Weekly Report",
                    subtitle = "Download PDF",
                    color = Color(0xFFFCE7F3),
                    iconColor = Color(0xFFEC4899),
                    onClick = { navController.navigate("weekly_report") },
                    modifier = Modifier.testTag("featureWeeklyReport")
                )

                Spacer(modifier = Modifier.height(24.dp))
                


                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}


private fun getMoodEmoji(mood: String): String {
    return when (mood.lowercase()) {
        "happy", "good", "great" -> "😊"
        "neutral", "okay", "fine", "calm" -> "😐"
        "sad", "bad", "unhappy" -> "😔"
        "anxious", "stressed" -> "😰"
        "excited" -> "🤩"
        "tired", "exhausted" -> "😴"
        else -> "😐"
    }
}

@Composable
fun SummaryCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

@Composable
fun BurnoutAlertBox(riskLevel: String, riskScore: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val gradient = when {
        riskScore > 75 -> Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))
        riskScore > 40 -> Brush.horizontalGradient(listOf(Color(0xFFFF7E3D), Color(0xFFF97316)))
        else -> Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
    }
    
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.background(gradient).padding(20.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(if (riskScore < 40) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Burnout Alert", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Risk Level: $riskLevel", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (riskScore > 75) "Immediate action required. Your burnout risk is critically high."
                    else if (riskScore > 40) "Your stress levels are elevated. Consider taking breaks and getting more sleep."
                    else "Your mental balance is good! Maintain your current routine.",
                    color = Color.White, fontSize = 14.sp, lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun FeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: String? = null,
    progress: Float? = null,
    color: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = color) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp)) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeColors.textPrimary)
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
                if (progress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = iconColor,
                        trackColor = iconColor.copy(alpha = 0.1f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            if (trailing != null) {
                Surface(color = iconColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(text = trailing, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = iconColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

/**
 * Compact App Usage format: "H.MM", where the digits after the dot are MINUTES,
 * not a decimal fraction of an hour.
 *
 *   4h 10m 40s -> "4.11H"   (40s rounds 10m up to 11m)
 *   1h 30m 00s -> "1.30H"
 *          45m -> "45m"     (under an hour, minutes only)
 *
 * Seconds are rounded to the nearest minute BEFORE splitting, so the total is
 * never truncated on its way to the display. Used only by the App Usage card;
 * formatDisplayTime below is unchanged and still serves the other cards.
 */
private fun formatCompactUsage(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "0m"
    val totalMinutes = (totalSeconds + 30L) / 60L   // integer round-half-up
    if (totalMinutes < 60L) return "${totalMinutes}m"
    val h = totalMinutes / 60L
    val m = totalMinutes % 60L
    return "$h.${m.toString().padStart(2, '0')}H"
}

private fun formatDisplayTime(seconds: Long): String {
    if (seconds < 3600) {
        val mins = (seconds / 60).toInt()
        return "${mins}m"
    }
    val hours = seconds / 3600f
    val formatted = (hours * 10).toInt() / 10f
    return "${formatted}H"
}

private fun formatProgressTime(seconds: Long): String {
    if (seconds < 3600) {
        val mins = (seconds / 60).toInt()
        return "${mins} min"
    }
    val hours = seconds / 3600f
    val formatted = (hours * 10).toInt() / 10f
    return "${formatted} hr"
}

private fun getRiskLevelName(score: Float): String = when {
    score > 75 -> "High"
    score > 40 -> "Moderate"
    else -> "Low"
}

private fun getFormattedExactDuration(hoursDecimal: Float, addedSeconds: Long): String {
    val totalSecs = (hoursDecimal * 3600).toLong() + addedSeconds
    return formatDisplayTime(totalSecs)
}
