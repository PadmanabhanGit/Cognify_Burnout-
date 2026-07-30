package com.simats.burnouttracker

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.simats.burnouttracker.data.api.ApiClient
import com.simats.burnouttracker.data.models.WeeklyReportData
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import kotlin.math.cos
import kotlin.math.sin

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
    val screenBgColor = Color(0xFFF9FAFB)

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
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.padding(6.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Weekly Mental\nHealth Report",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 30.sp
                    )
                    Text(
                        text = reportData?.let { "${it.period.from} - ${it.period.to}" } ?: "Loading...",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
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
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SummaryItem(label = "TOTAL STUDY TIME", value = "32h 15m", trend = "+12%", trendColor = Color(0xFF10B981), modifier = Modifier.weight(1f))
                        SummaryItem(label = "AVG SLEEP", value = "6h 45m", trend = "-5%", trendColor = Color(0xFFEF4444), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SummaryItem(label = "AVG MOOD", value = "7.2/10", modifier = Modifier.weight(1f))
                        SummaryItem(label = "PRODUCTIVITY", value = "84%", modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        color = Color(0xFFFFF7ED),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "BURNOUT RISK LEVEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
                                Text(text = "Moderate", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                                Text(
                                    text = "Slight increase in fatigue levels recently. Consider taking a rest day.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9A3412).copy(alpha = 0.7f)
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
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    ActivityChart(modifier = Modifier.fillMaxWidth().height(150.dp))
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
                        Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    LineChart(modifier = Modifier.fillMaxWidth().height(150.dp))
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
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    RadarChart(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        labels = listOf("Physical", "Mental", "Social", "Focus", "Sleep", "Balance"),
                        dataPoints = listOf(0.7f, 0.8f, 0.5f, 0.9f, 0.6f, 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChartLegend(color = Color(0xFF9333EA), label = "This Week")
                        Spacer(modifier = Modifier.width(20.dp))
                        ChartLegend(color = Color(0xFFE5E7EB), label = "Last Week")
                    }
                }

                // Achievements
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Achievements", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AchievementCard(icon = Icons.Default.FlashOn, title = "Focus Master", subtitle = "15h Focus Time", color = Color(0xFFF97316), modifier = Modifier.weight(1f))
                    AchievementCard(icon = Icons.Default.NightsStay, title = "Zen Mind", subtitle = "Early Meditation", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AchievementCard(icon = Icons.Default.Lock, title = "Early Bird", subtitle = "7 days in a row", color = Color(0xFFD1D5DB), modifier = Modifier.weight(1f), isLocked = true)
                    AchievementCard(icon = Icons.Default.Lock, title = "Steady Pro", subtitle = "No skips in 10d", color = Color(0xFFD1D5DB), modifier = Modifier.weight(1f), isLocked = true)
                }

                // Areas of Concern
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrackChanges, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Areas of Concern", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                ConcernItem(title = "Sleep Deficit", subtitle = "Avg hours below 7h for 3 days", tag = "HIGH", tagColor = Color(0xFFFEE2E2), tagTextColor = Color(0xFFEF4444))
                ConcernItem(title = "Late Night Screen Time", subtitle = "Found usage past 12 PM", tag = "MEDIUM", tagColor = Color(0xFFFFEDD5), tagTextColor = Color(0xFFF97316))

                // Personalized Recommendations
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                    modifier = Modifier.background(reportGradient, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Personalized\nRecommendations", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        RecommendationBullet("Take short breaks every 45 mins for eye wellness.")
                        RecommendationBullet("Establish a soft-relax wind-down routine before sleep.")
                        RecommendationBullet("Prioritize sleep health to sustain further learning.")
                    }
                }

                // Goals for Next Week
                ReportCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Goals for Next Week", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    GoalItem("Achieve 7.5 hours of sleep on 5+ nights")
                    GoalItem("Maintain study sessions within 6 hours daily")
                    GoalItem("Practice stress management behaviors daily")
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
        Text(text = label, fontSize = 10.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
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
        Text(text = label, fontSize = 12.sp, color = Color(0xFF6B7280))
    }
}

@Composable
fun AchievementCard(icon: ImageVector, title: String, subtitle: String, color: Color, modifier: Modifier = Modifier, isLocked: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isLocked) Color(0xFFF9FAFB) else Color(0xFFFFFBEB),
        border = if (isLocked) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEF3C7))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = if (isLocked) Color(0xFFE5E7EB) else color, modifier = Modifier.size(40.dp)) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.padding(10.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isLocked) Color(0xFF9CA3AF) else Color(0xFF1F2937))
            Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF6B7280))
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
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1F2937))
                Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF6B7280))
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

@Composable
fun ActivityChart(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val days = 7
        val spacing = size.width / (days + 1)
        val barWidth = 12.dp.toPx()
        
        for (i in 0 until days) {
            val x = spacing * (i + 1)
            // Study Bar
            val studyHeight = size.height * (0.4f + (i % 3) * 0.2f)
            drawRect(
                color = Color(0xFF3B82F6),
                topLeft = Offset(x - barWidth, size.height - studyHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, studyHeight)
            )
            // Sleep Bar
            val sleepHeight = size.height * (0.3f + (i % 2) * 0.3f)
            drawRect(
                color = Color(0xFFA855F7),
                topLeft = Offset(x, size.height - sleepHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, sleepHeight)
            )
        }
    }
}

@Composable
fun LineChart(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val points = 7
        val spacing = size.width / (points - 1)
        
        val moodPath = Path()
        val prodPath = Path()
        
        for (i in 0 until points) {
            val x = i * spacing
            val moodY = size.height * (0.2f + (i % 3) * 0.15f)
            val prodY = size.height * (0.1f + (i % 2) * 0.4f)
            
            if (i == 0) {
                moodPath.moveTo(x, moodY)
                prodPath.moveTo(x, prodY)
            } else {
                moodPath.lineTo(x, moodY)
                prodPath.lineTo(x, prodY)
            }
            drawCircle(Color(0xFFEC4899), 4.dp.toPx(), Offset(x, moodY))
            drawCircle(Color(0xFF10B981), 4.dp.toPx(), Offset(x, prodY))
        }
        
        drawPath(moodPath, Color(0xFFEC4899), style = Stroke(width = 2.dp.toPx()))
        drawPath(prodPath, Color(0xFF10B981), style = Stroke(width = 2.dp.toPx()))
    }
}

@Preview
@Composable
fun WeeklyReportScreenPreview() {
    WeeklyReportScreen(rememberNavController())
}
