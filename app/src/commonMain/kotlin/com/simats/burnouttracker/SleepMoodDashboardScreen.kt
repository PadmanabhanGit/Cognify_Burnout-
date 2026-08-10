package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

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
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.simats.burnouttracker.data.rememberSleepRepository
import com.simats.burnouttracker.ui.SleepViewModel
import com.simats.burnouttracker.utils.formatMinutes
import com.simats.burnouttracker.utils.formatTimestamp

@Composable
fun SleepMoodDashboardScreen(navController: NavController) {
    val repository = rememberSleepRepository()
    val viewModel = remember { SleepViewModel(repository) }
    val sessions by viewModel.sessions.collectAsState()
    val latestSession = sessions.firstOrNull()
    val available = latestSession != null

    val wakeEvents by viewModel.selectedWakeEvents.collectAsState()

    val headerGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF4F46E5), Color(0xFF9333EA))
    )

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    LaunchedEffect(latestSession) {
        latestSession?.let {
            viewModel.selectSession(it.id)
        }
    }

    Scaffold(
        bottomBar = { SleepMoodBottomNavigation(navController) }
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
                    .height(200.dp)
                    .background(headerGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 40.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Sleep Analysis",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Detailed breakdown of your automatically detected sleep",
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
                if (!available) {
                    // No automatic session at all — explicit unavailable state.
                    // No gauge, no metric grid, no "--" scattered across a full
                    // layout: the whole analysis section is unavailable, so it
                    // says so once rather than showing an empty-looking dashboard.
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Bedtime, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Sleep analysis unavailable",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No detected sleep session is available yet.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    val session = latestSession!!

                    // Sleep Quality Score Card — session.sleepQuality is the engine's
                    // own 0-100 score (100 - disturbanceScore, computed in
                    // SleepMonitoringEngine.kt). Displayed as-is; no second formula.
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                                CircularProgressIndicator(
                                    progress = { session.sleepQuality / 100f },
                                    modifier = Modifier.fillMaxSize(),
                                    color = getQualityColor(session.sleepQuality),
                                    strokeWidth = 10.dp,
                                    trackColor = ThemeColors.background
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${session.sleepQuality}%",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ThemeColors.textPrimary
                                    )
                                    Text(
                                        text = getQualityLevel(session.sleepQuality),
                                        fontSize = 12.sp,
                                        color = getQualityColor(session.sleepQuality),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MetricCard(
                                    label = "Total Sleep",
                                    value = formatMinutes(session.totalSleepMinutes),
                                    icon = Icons.Default.Bedtime,
                                    color = Color(0xFF6366F1),
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    label = "Awakenings",
                                    value = "${session.awakeningCount}",
                                    icon = Icons.Default.NotificationsActive,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    label = "Disturbance",
                                    value = "${session.disturbanceScore}",
                                    icon = Icons.Default.Warning,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Sleep Start & Wake Times
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TimeInfo(label = "Sleep Start", time = formatTimestamp(session.sleepStart), icon = Icons.Default.Nightlight)
                            VerticalDivider(modifier = Modifier.height(40.dp), thickness = 1.dp, color = ThemeColors.background)
                            TimeInfo(label = "Wake Up", time = formatTimestamp(session.sleepEnd), icon = Icons.Default.WbSunny)
                        }
                    }

                    // Timeline Section — real sleepStart/sleepEnd and real WakeEvent
                    // rows only (from Room wake_events, populated by the engine's
                    // actual awakening detection). No "Monitoring Started" node:
                    // there is no persisted monitoring-start timestamp anywhere in
                    // the data model, so the previous hardcoded "10:00 PM" here had
                    // nothing real to represent.
                    Text(
                        text = "Sleep Timeline",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColors.textPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            TimelineItem(
                                time = formatTimestamp(session.sleepStart),
                                title = "Sleep Started",
                                subtitle = "Detected from a sustained inactivity gap",
                                icon = Icons.Default.Bedtime,
                                color = Color(0xFF4F46E5)
                            )

                            wakeEvents.forEach { event ->
                                TimelineItem(
                                    time = formatTimestamp(event.timestamp),
                                    title = "Awakening: ${event.appName}",
                                    subtitle = "Duration: ${event.duration / 60000} mins (${event.category})",
                                    icon = Icons.Default.NotificationsActive,
                                    color = Color(0xFFF59E0B)
                                )
                            }

                            TimelineItem(
                                time = formatTimestamp(session.sleepEnd),
                                title = "Final Wake Up",
                                subtitle = "Detected from a sustained activity cluster",
                                icon = Icons.Default.WbSunny,
                                color = Color(0xFF10B981),
                                isLast = true
                            )
                        }
                    }
                }

                Button(
                    onClick = { navController.navigate("sleep_mood_analytics") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("View Full Analytics", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun TimeInfo(label: String, time: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, fontSize = 12.sp, color = Color.Gray)
        }
        Text(text = time, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
    }
}

private fun getQualityLevel(score: Int): String = when {
    score >= 90 -> "Excellent"
    score >= 75 -> "Good"
    score >= 60 -> "Moderate"
    score >= 40 -> "Poor"
    else -> "Very Poor"
}

private fun getQualityColor(score: Int): Color = when {
    score >= 75 -> Color(0xFF10B981)
    score >= 60 -> Color(0xFFF59E0B)
    else -> Color(0xFFEF4444)
}

@Preview
@Composable
fun SleepMoodDashboardScreenPreview() {
    SleepMoodDashboardScreen(rememberNavController())
}
