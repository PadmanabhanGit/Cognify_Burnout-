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
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.utils.AppData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTrackerDetailsScreen(navController: NavController) {
    val screenBgColor = Color(0xFFF9FAFB)

    Scaffold(
        containerColor = screenBgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Study Track", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Monthly Trend Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Monthly Trend", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Monthly Trend Chart
                    MonthlyTrendChart()
                }
            }

            // 2. Study Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Study Breakdown", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937))
                        }
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val subjectColors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFF10B981), Color(0xFFF97316))
                    val subjects = AppData.studyBreakdown.keys.toList()
                    
                    if (subjects.isEmpty()) {
                        Text("No study data recorded yet.", color = Color.Gray, fontSize = 14.sp)
                    }

                    subjects.forEachIndexed { index, subject ->
                        val hours = AppData.studyBreakdown[subject] ?: 0f
                        SubjectProgressItem(
                            label = subject,
                            hours = "${((hours * 10).toInt() / 10f)}h",
                            progress = hours / 15f,
                            color = subjectColors[index % subjectColors.size]
                        )
                        if (index < subjects.size - 1) Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(30.dp))
                    
                    // Total Study Time Summary
                    Surface(
                        color = Color(0xFFF5F3FF),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Total Study Time", fontWeight = FontWeight.Medium, color = Color(0xFF4B5563))
                            Text(text = "${(AppData.studyWeekHours * 10).toInt() / 10f}h", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7C3AED))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun MonthlyTrendChart() {
    val points = AppData.monthlyStudyTrend
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val width = size.width
        val height = size.height - 40.dp.toPx()
        val spacing = width / (points.size - 1).coerceAtLeast(1)
        
        val path = Path()
        
        points.forEachIndexed { index, value ->
            val x = index * spacing
            val y = height - (value * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = Color(0xFF8B5CF6),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        
        points.forEachIndexed { index, value ->
            val x = index * spacing
            val y = height - (value * height)
            drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(x, y))
            drawCircle(Color(0xFF8B5CF6), radius = 7.dp.toPx(), center = Offset(x, y), style = Stroke(width = 2.dp.toPx()))
            drawCircle(Color(0xFF8B5CF6), radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("Week 1", "Week 2", "Week 3", "Week 4").forEach { week ->
            Text(text = week, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SubjectProgressItem(label: String, hours: String, progress: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4B5563))
            Text(text = hours, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = color,
            trackColor = Color(0xFFF3F4F6),
            strokeCap = StrokeCap.Round
        )
    }
}
