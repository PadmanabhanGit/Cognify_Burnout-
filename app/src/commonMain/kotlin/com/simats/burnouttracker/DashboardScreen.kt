package com.simats.burnouttracker

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
    val firstName = remember(settings) { settings.getString("firstName", "Student") ?: "Student" }
    
    val predictor = rememberBurnoutPredictor()
    val usageHelper = rememberUsageStatsHelper()
    val sleepRepository = rememberSleepRepository()
    val sleepSessions by sleepRepository.getRecentSessions().collectAsState(emptyList())
    val latestSleepSession = sleepSessions.firstOrNull()
    
    var riskScore by remember { mutableStateOf(AppData.predictedScore) }
    var riskLevel by remember { mutableStateOf(getRiskLevelName(riskScore)) }
    var currentDate by remember { mutableStateOf(formatDashboardDate()) }
    
    LaunchedEffect(latestSleepSession) {
        latestSleepSession?.let {
            AppData.lastSleepLogged = it.totalSleepMinutes / 60f
        }
    }
    
    LaunchedEffect(Unit) {
        while(true) {
            currentDate = formatDashboardDate()
            if (usageHelper.hasUsageStatsPermission()) {
                val realFeatures = usageHelper.fetchDailyUsage()
                AppData.currentFeatures = realFeatures
                val prediction = predictor.predict(realFeatures)
                AppData.predictedScore = prediction
                riskScore = prediction
                riskLevel = getRiskLevelName(prediction)
                
                // Sync prediction to backend
                try {
                    ApiClient.saveBurnoutAssessment(
                        com.simats.burnouttracker.data.models.BurnoutAssessmentRequest(
                            riskScore = prediction.toInt(),
                            riskLevel = riskLevel.lowercase(),
                            warnings = if (prediction > 40) listOf("Elevated stress levels detected") else emptyList(),
                            recommendations = if (prediction > 40) listOf("Take a short break", "Practice mindfulness") else emptyList()
                        )
                    )
                } catch (e: Exception) {
                    // Fail silently
                }

                // Add a mock log if empty
                if (AppData.sleepLogs.isEmpty()) {
                    AppData.sleepLogs.add(
                        SleepLog("Feb 24", 8.0f, "😊", "Excellent", Color(0xFF10B981))
                    )
                    AppData.sleepLogs.add(
                        SleepLog("Feb 23", 6.5f, "😐", "Fair", Color(0xFFF97316))
                    )
                }

                AppData.hasData = true
                AppData.lastUpdatedTime = formatCurrentTime()
            }
            
            ApiClient.getDashboard()
            delay(30000) // Refresh every 30 seconds for "real-time" feel
        }
    }

    val headerGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF6B21A8), Color(0xFF3B82F6))
    )

    Scaffold(
        containerColor = Color(0xFFF9FAFB),
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
                    val totalStudyHours = AppData.studyTodayHours
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            icon = Icons.Default.AccessTime,
                            value = formatHours(totalStudyHours),
                            label = "Study Today",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            icon = Icons.Default.Bedtime,
                            value = formatMinutes(latestSleepSession?.totalSleepMinutes ?: (AppData.lastSleepLogged * 60).toInt()),
                            label = "Sleep",
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
                    color = Color(0xFF1F2937),
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )

                // Feature List
                val totalStudyHours = AppData.studyTodayHours
                FeatureCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Study Tracking",
                    subtitle = "Daily goal progress",
                    trailing = formatHours(totalStudyHours),
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
                    trailing = AppData.sleepLogs.firstOrNull()?.status ?: "Log Today",
                    progress = (latestSleepSession?.sleepQuality?.toFloat() ?: 0f) / 100f,
                    color = Color(0xFFEEF2FF),
                    iconColor = Color(0xFF6366F1),
                    onClick = { navController.navigate("sleep_mood") },
                    modifier = Modifier.testTag("featureSleepMood")
                )

                FeatureCard(
                    icon = Icons.Default.BarChart,
                    title = "App Usage",
                    subtitle = "Leisure time impact",
                    trailing = formatHours(AppData.currentFeatures.socialHours + AppData.currentFeatures.gamingHours + AppData.currentFeatures.streamingHours),
                    progress = ((AppData.currentFeatures.socialHours + AppData.currentFeatures.gamingHours + AppData.currentFeatures.streamingHours) / 10f).coerceIn(0f, 1f),
                    color = Color(0xFFF5F3FF),
                    iconColor = Color(0xFF8B5CF6),
                    onClick = { navController.navigate("entertainment_usage") },
                    modifier = Modifier.testTag("featureAppUsage")
                )

                FeatureCard(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = "Productivity",
                    subtitle = "Weekly trends",
                    trailing = "+12%",
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
fun FeatureCard(icon: ImageVector, title: String, subtitle: String, trailing: String? = null, progress: Float? = null, color: Color, iconColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = color) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp)) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
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

private fun getRiskLevelName(score: Float): String = when {
    score > 75 -> "High"
    score > 40 -> "Moderate"
    else -> "Low"
}
