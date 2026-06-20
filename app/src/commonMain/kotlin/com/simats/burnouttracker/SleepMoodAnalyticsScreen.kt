package com.simats.burnouttracker

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.data.rememberSleepRepository
import com.simats.burnouttracker.ui.SleepViewModel

@Composable
fun SleepMoodAnalyticsScreen(navController: NavController) {
    val repository = rememberSleepRepository()
    val viewModel = remember { SleepViewModel(repository) }
    val sessions by viewModel.sessions.collectAsState()

    val avg7Day = if (sessions.isNotEmpty()) sessions.take(7).map { it.sleepQuality }.average().toInt() else 0
    val avg30Day = if (sessions.isNotEmpty()) sessions.take(30).map { it.sleepQuality }.average().toInt() else 0

    val headerGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF4F46E5), Color(0xFF9333EA))
    )

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(headerGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 40.dp, start = 24.dp, end = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", tint = Color.White)
                    }
                    Text("Sleep Analytics", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier.padding(24.dp).offset(y = (-20).dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Averages Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("7-Day Avg", fontSize = 12.sp, color = Color.Gray)
                            Text("$avg7Day%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                        }
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("30-Day Avg", fontSize = 12.sp, color = Color.Gray)
                            Text("$avg30Day%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9333EA))
                        }
                    }
                }

                // Trend Graph
                SleepMoodTrendCard(
                    title = "Quality Trends",
                    icon = Icons.Default.ShowChart,
                    color = Color(0xFF4F46E5),
                    dataPoints = sessions.take(7).map { it.sleepQuality / 100f }.reversed(),
                    showFill = true
                )

                // Night Usage Breakdown
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Top Disturbing Factors", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        FactorItem("Night Social Media", "Heavy Impact", Color(0xFFEF4444))
                        FactorItem("Late Screen Usage", "Moderate Impact", Color(0xFFF59E0B))
                        FactorItem("Irregular Start Time", "Low Impact", Color(0xFF6366F1))
                    }
                }
            }
        }
    }
}

@Composable
fun FactorItem(label: String, impact: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF374151))
        Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
            Text(impact, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}
