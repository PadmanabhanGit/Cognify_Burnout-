package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.simats.burnouttracker.data.api.ApiClient
import com.simats.burnouttracker.data.models.DailyActivityPoint
import com.simats.burnouttracker.data.models.MoodVsProductivityPoint
import com.simats.burnouttracker.data.models.WeeklyReportData
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import kotlin.math.cos
import kotlin.math.sin
import com.simats.burnouttracker.utils.AppData

@Composable
fun WeeklyReportScreen(navController: NavController) {
    var reportData by remember { mutableStateOf<WeeklyReportData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val response = ApiClient.getWeeklyReport()
            if (response.success) {
                reportData = response.report
            }
        } catch (e: Exception) {
        } finally {
            isLoading = false
        }
    }

    val reportGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF9333EA), Color(0xFFDB2777))
    )
    val screenBgColor = ThemeColors.background

    Scaffold(
        containerColor = screenBgColor,
        bottomBar = { AppBottomNavigation(navController, "weekly_report") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(reportGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 40.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { navController.popBackStack() }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Weekly Report",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 30.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReportActionButton(icon = Icons.Default.Download, label = "Download PDF")
                        ReportActionButton(icon = Icons.Default.Share, label = "Share Report")
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-20).dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Executive Summary Card
                ReportCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Executive Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    // Real trailing-7-day figures from GET /api/report/weekly — not
                    // today's live AppData snapshot relabeled as a weekly figure.
                    // "--" while the fetch is in flight or failed; never a stale
                    // live value pretending to be the week's number.
                    val summary = reportData?.summary
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val studyH = summary?.totalStudyHours?.let { formatDisplayTime((it * 3600).toLong()) } ?: "--"
                        SummaryItem(label = "TOTAL STUDY TIME", value = studyH, trend = "", trendColor = Color(0xFF10B981), modifier = Modifier.weight(1f))
                        val sleepH = summary?.avgSleep?.let { formatDisplayTime((it * 3600).toLong()) } ?: "--"
                        SummaryItem(label = "AVG SLEEP", value = sleepH, trend = "", trendColor = Color(0xFFEF4444), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val formattedMood = summary?.avgMood?.let { (it * 10).toInt() / 10.0 }
                        SummaryItem(label = "AVG MOOD", value = formattedMood?.let { "$it/10" } ?: "--", modifier = Modifier.weight(1f))
                        val formattedProductivity = summary?.avgProductivity?.toInt()
                        SummaryItem(label = "PRODUCTIVITY", value = formattedProductivity?.let { "$it%" } ?: "--", modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    val riskLevel = when {
                        AppData.predictedScore < 30f -> "Low"
                        AppData.predictedScore < 70f -> "Moderate"
                        else -> "High"
                    }
                    val riskColor = when (riskLevel) {
                        "Low" -> Color(0xFF10B981)
                        "Moderate" -> Color(0xFFF97316)
                        "High" -> Color(0xFFEF4444)
                        else -> Color(0xFFF97316)
                    }
                    Surface(
                        color = riskColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = riskColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "BURNOUT RISK LEVEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = riskColor)
                                Text(text = riskLevel, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = riskColor)
                                Text(
                                    text = "Based on your recent usage data.",
                                    fontSize = 11.sp,
                                    color = riskColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Daily Activity Breakdown
                ReportCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Daily Activity Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = ThemeColors.textTertiary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    if (reportData == null && !isLoading) {
                        Text(
                            text = "Couldn't load this week's activity.",
                            fontSize = 12.sp,
                            color = ThemeColors.textTertiary,
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
                    } else {
                        ActivityChart(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            days = reportData?.dailyActivity ?: emptyList()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChartLegend(color = Color(0xFF3B82F6), label = "Study")
                        Spacer(modifier = Modifier.width(20.dp))
                        ChartLegend(color = Color(0xFFA855F7), label = "Sleep")
                    }
                }

                // Mood & Productivity
                ReportCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Mood & Productivity", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Icon(Icons.Default.Timeline, contentDescription = null, tint = ThemeColors.textTertiary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    if (reportData == null && !isLoading) {
                        Text(
                            text = "Couldn't load this week's mood/productivity data.",
                            fontSize = 12.sp,
                            color = ThemeColors.textTertiary,
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
                    } else {
                        MoodProductivityChart(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            days = reportData?.moodVsProductivity ?: emptyList()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChartLegend(color = Color(0xFFEC4899), label = "Mood Score")
                        Spacer(modifier = Modifier.width(20.dp))
                        ChartLegend(color = Color(0xFF10B981), label = "Productivity %")
                    }
                }

                // Wellness Comparison (Spider)
                ReportCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Wellness Comparison", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    val mentalScore = (100f - AppData.predictedScore) / 100f
                    val socialScore = (1f - (AppData.currentFeatures.socialHours / 10f)).coerceIn(0.1f, 1f)
                    val focusScore = (AppData.currentFeatures.productivityHours / 8f).coerceIn(0.1f, 1f)
                    val sleepScore = (AppData.lastSleepLogged / 8f).coerceIn(0.1f, 1f)
                    val physicalScore = 0.6f // Mocked until Health Connect is implemented
                    RadarChart(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        labels = listOf("Physical", "Mental", "Social", "Focus", "Sleep"),
                        dataPoints = listOf(physicalScore, mentalScore, socialScore, focusScore, sleepScore)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChartLegend(color = Color(0xFF9333EA), label = "This Week")
                        Spacer(modifier = Modifier.width(20.dp))
                        ChartLegend(color = ThemeColors.border, label = "Last Week")
                    }
                }

                // Achievements
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Achievements", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val hasFocusMaster = AppData.studyTodayHours >= 4f
                    AchievementCard(icon = Icons.Default.FlashOn, title = "Focus Master", subtitle = if (hasFocusMaster) "High Focus Today" else "Locked", color = Color(0xFFF97316), modifier = Modifier.weight(1f), isLocked = !hasFocusMaster)
                    
                    val hasZenMind = AppData.lastSleepLogged >= 7f
                    AchievementCard(icon = Icons.Default.NightsStay, title = "Zen Mind", subtitle = if (hasZenMind) "Good Sleep" else "Locked", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f), isLocked = !hasZenMind)
                }

                // Areas of Concern
                val showSleepDeficit = AppData.lastSleepLogged < 6f
                val showBurnout = AppData.predictedScore > 50f
                if (showSleepDeficit || showBurnout) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrackChanges, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Areas of Concern", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    if (showSleepDeficit) {
                        ConcernItem(title = "Sleep Deficit", subtitle = "Logged below 6h recently", tag = "HIGH", tagColor = Color(0xFFFEE2E2), tagTextColor = Color(0xFFEF4444))
                    }
                    if (showBurnout) {
                        ConcernItem(title = "High Burnout Risk", subtitle = "Your usage suggests burnout", tag = "MEDIUM", tagColor = Color(0xFFFFEDD5), tagTextColor = Color(0xFFF97316))
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ReportCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun ReportActionButton(icon: ImageVector, label: String) {
    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String, trend: String? = null, trendColor: Color = Color.Gray, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 10.sp, color = ThemeColors.textSecondary, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
            if (trend != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = trend, fontSize = 10.sp, color = trendColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 12.sp, color = ThemeColors.textSecondary)
    }
}

@Composable
fun AchievementCard(icon: ImageVector, title: String, subtitle: String, color: Color, modifier: Modifier = Modifier, isLocked: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isLocked) ThemeColors.background else Color(0xFFFFFBEB),
        border = if (isLocked) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEF3C7))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = if (isLocked) ThemeColors.border else color, modifier = Modifier.size(40.dp)) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.padding(10.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isLocked) ThemeColors.textTertiary else ThemeColors.textPrimary)
            Text(text = subtitle, fontSize = 10.sp, color = ThemeColors.textSecondary)
        }
    }
}

@Composable
fun ConcernItem(title: String, subtitle: String, tag: String, tagColor: Color, tagTextColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ThemeColors.textPrimary)
                Text(text = subtitle, fontSize = 12.sp, color = ThemeColors.textSecondary)
            }
            Surface(color = tagColor, shape = RoundedCornerShape(6.dp)) {
                Text(text = tag, color = tagTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
fun RecommendationBullet(text: String) {
    Row(modifier = Modifier.padding(bottom = 12.dp)) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
fun GoalItem(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        color = Color(0xFFEFF6FF),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(4.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)), modifier = Modifier.size(18.dp)) {}
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, fontSize = 13.sp, color = Color(0xFF1E40AF))
        }
    }
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

/** Draws real per-day study/sleep bars from the backend's trailing-7-day
 *  breakdown. A day with no record for that series draws no bar for it —
 *  never a fabricated height. Bar heights are relative to a fixed reference
 *  (4h study, 10h sleep = full height), not the data's own max, so a single
 *  heavy day cannot visually flatten the rest of the week. */
@Composable
fun ActivityChart(modifier: Modifier = Modifier, days: List<DailyActivityPoint>) {
    Canvas(modifier = modifier) {
        if (days.isEmpty()) return@Canvas
        val spacing = size.width / (days.size + 1)
        val barWidth = 12.dp.toPx()
        val maxStudyMinutes = 240f
        val maxSleepHours = 10f

        days.forEachIndexed { i, day ->
            val x = spacing * (i + 1)
            day.studyMinutes?.let { minutes ->
                val h = size.height * (minutes / maxStudyMinutes).coerceIn(0f, 1f)
                drawRect(
                    color = Color(0xFF3B82F6),
                    topLeft = Offset(x - barWidth, size.height - h),
                    size = androidx.compose.ui.geometry.Size(barWidth, h)
                )
            }
            day.sleepHours?.let { hours ->
                val h = size.height * (hours.toFloat() / maxSleepHours).coerceIn(0f, 1f)
                drawRect(
                    color = Color(0xFFA855F7),
                    topLeft = Offset(x, size.height - h),
                    size = androidx.compose.ui.geometry.Size(barWidth, h)
                )
            }
        }
    }
}

/** Draws real per-day mood/productivity points from the backend's trailing-
 *  7-day breakdown. A line segment is only drawn between two CONSECUTIVE
 *  days that both have a value for that series — a day with no record is a
 *  gap, never interpolated or treated as zero. */
@Composable
fun MoodProductivityChart(modifier: Modifier = Modifier, days: List<MoodVsProductivityPoint>) {
    Canvas(modifier = modifier) {
        val n = days.size
        if (n < 2) return@Canvas
        val spacing = size.width / (n - 1)

        fun moodY(score: Int) = size.height - ((score / 10f) * size.height)
        fun prodY(score: Int) = size.height - ((score / 100f) * size.height)

        var prevMoodIndex: Int? = null
        var prevProdIndex: Int? = null
        days.forEachIndexed { i, day ->
            val x = i * spacing
            day.moodScore?.let { mood ->
                val y = moodY(mood)
                val prevIndex = prevMoodIndex
                if (prevIndex == i - 1) {
                    val prevScore = days[prevIndex].moodScore
                    if (prevScore != null) {
                        drawLine(Color(0xFFEC4899), Offset(prevIndex * spacing, moodY(prevScore)), Offset(x, y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
                drawCircle(Color(0xFFEC4899), 4.dp.toPx(), Offset(x, y))
                prevMoodIndex = i
            }
            day.productivityScore?.let { prod ->
                val y = prodY(prod)
                val prevIndex = prevProdIndex
                if (prevIndex == i - 1) {
                    val prevScore = days[prevIndex].productivityScore
                    if (prevScore != null) {
                        drawLine(Color(0xFF10B981), Offset(prevIndex * spacing, prodY(prevScore)), Offset(x, y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
                drawCircle(Color(0xFF10B981), 4.dp.toPx(), Offset(x, y))
                prevProdIndex = i
            }
        }
    }
}

@Preview
@Composable
fun WeeklyReportScreenPreview() {
    WeeklyReportScreen(rememberNavController())
}
