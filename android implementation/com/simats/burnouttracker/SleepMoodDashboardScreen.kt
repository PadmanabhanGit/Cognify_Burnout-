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
    
    // Sync fallback from AI engine if real session not yet analyzed
    val displayQuality = latestSession?.sleepQuality ?: (100 - com.simats.burnouttracker.utils.AppData.predictedScore).toInt()
    val displayDisturbance = latestSession?.disturbanceScore ?: (com.simats.burnouttracker.utils.AppData.predictedScore * 2.5f).toInt()
    
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
                .background(Color(0xFFF9FAFB))
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
                        text = "Sleep Monitoring",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Scientific analysis of your night rest",
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
                // Sleep Quality Score Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                            CircularProgressIndicator(
                                progress = { displayQuality / 100f },
                                modifier = Modifier.fillMaxSize(),
                                color = getQualityColor(displayQuality),
                                strokeWidth = 10.dp,
                                trackColor = Color(0xFFF3F4F6)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$displayQuality%",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1F2937)
                                )
                                Text(
                                    text = getQualityLevel(displayQuality),
                                    fontSize = 12.sp,
                                    color = getQualityColor(displayQuality),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricCard(
                                label = "Total Sleep",
                                value = formatMinutes(latestSession?.totalSleepMinutes ?: (com.simats.burnouttracker.utils.AppData.lastSleepLogged * 60).toInt()),
                                icon = Icons.Default.Bedtime,
                                color = Color(0xFF6366F1),
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Awakenings",
                                value = "${latestSession?.awakeningCount ?: 0}",
                                icon = Icons.Default.NotificationsActive,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Disturbance",
                                value = "$displayDisturbance",
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
                        TimeInfo(label = "Sleep Start", time = formatTimestamp(latestSession?.sleepStart ?: 0), icon = Icons.Default.Nightlight)
                        VerticalDivider(modifier = Modifier.height(40.dp), thickness = 1.dp, color = Color(0xFFF3F4F6))
                        TimeInfo(label = "Wake Up", time = formatTimestamp(latestSession?.sleepEnd ?: 0), icon = Icons.Default.WbSunny)
                    }
                }

                // Timeline Section
                Text(
                    text = "Sleep Timeline",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
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
                            time = "10:00 PM",
                            title = "Monitoring Started",
                            icon = Icons.Default.RadioButtonChecked,
                            color = Color(0xFF6366F1)
                        )
                        
                        if (latestSession != null) {
                            TimelineItem(
                                time = formatTimestamp(latestSession.sleepStart),
                                title = "Sleep Started",
                                subtitle = "User became inactive for 20+ mins",
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
                                time = formatTimestamp(latestSession.sleepEnd),
                                title = "Final Wake Up",
                                subtitle = "Monitoring successfully completed",
                                icon = Icons.Default.WbSunny,
                                color = Color(0xFF10B981),
                                isLast = true
                            )
                        } else {
                            Text(text = "No timeline data for today.", color = Color.Gray, fontSize = 14.sp)
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
        Text(text = time, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
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
