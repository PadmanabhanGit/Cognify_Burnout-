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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.data.api.ApiClient
import com.simats.burnouttracker.data.models.ProductivityLogRequest
import com.simats.burnouttracker.utils.AppData
import kotlinx.coroutines.launch

@Composable
fun ProductivityScreen(navController: NavController) {
    val greenGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF10B981), Color(0xFF059669))
    )
    val screenBgColor = Color(0xFFF9FAFB)
    val scope = rememberCoroutineScope()

    // Sync productivity data periodically
    LaunchedEffect(Unit) {
        try {
            ApiClient.logProductivity(
                ProductivityLogRequest(
                    productivityScore = AppData.productivityScore,
                    focusHours = AppData.peakFocusHours.toDouble(),
                    tasksCompleted = (AppData.productivityScore / 10), // Mock
                    tasksPlanned = 10
                )
            )
        } catch (e: Exception) {}
    }

    Scaffold(
        containerColor = screenBgColor,
        bottomBar = { AppBottomNavigation(navController, "productivity") }
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
                    .height(180.dp)
                    .background(greenGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 40.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Productivity Analysis",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Track and optimize your performance",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-30).dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Today's Productivity Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Today's Productivity", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(30.dp))
                        
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(160.dp)) {
                                drawArc(Color(0xFFF3F4F6), 140f, 260f, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                                drawArc(Color(0xFF0F172A), 140f, 260f * (AppData.productivityScore / 100f), false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = AppData.productivityScore.toString(), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111827))
                                Text(text = "PRODUCTIVITY\nSCORE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF), textAlign = TextAlign.Center)
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ChangeBox(label = "Weekly Change", value = "+12%", color = Color(0xFFDCFCE7), textColor = Color(0xFF16A34A), modifier = Modifier.weight(1f))
                            ChangeBox(label = "This Month", value = "+6%", color = Color(0xFFEFF6FF), textColor = Color(0xFF2563EB), modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 2. 7-Day Trend Analysis Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "7-Day Trend Analysis", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                        Spacer(modifier = Modifier.height(24.dp))
                        MultiLineChart()
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            ProductivityLegendItem(Color(0xFF10B981), "Overall")
                            Spacer(modifier = Modifier.width(16.dp))
                            ProductivityLegendItem(Color(0xFF3B82F6), "Focus")
                            Spacer(modifier = Modifier.width(16.dp))
                            ProductivityLegendItem(Color(0xFF8B5CF6), "Efficiency")
                        }
                    }
                }

                // 3. Key Insights Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InsightMiniCard(icon = Icons.Default.FlashOn, value = "${AppData.peakFocusHours}h", label = "Peak Focus", sub = "Highest continuous span", modifier = Modifier.weight(1f))
                    InsightMiniCard(icon = Icons.Default.CheckCircle, value = "${AppData.goalHitRate}%", label = "Goal Hit", sub = "Daily target completion", modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InsightMiniCard(icon = Icons.Default.AccessTime, value = AppData.averageStartTime, label = "Start Time", sub = "Consistent start habit", modifier = Modifier.weight(1f))
                    InsightMiniCard(icon = Icons.Default.EmojiEvents, value = AppData.userGlobalRanking, label = "Ranking", sub = "Compared to peers", modifier = Modifier.weight(1f))
                }

                // 4. Peak Performance Hours Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Peak Performance Hours", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                        Spacer(modifier = Modifier.height(24.dp))
                        PeakAreaChart()
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("06AM", "09AM", "12PM", "03PM", "06PM", "09PM").forEach {
                                Text(text = it, fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // 5. Time Distribution Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Time Distribution", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                        Spacer(modifier = Modifier.height(20.dp))
                        DistributionItem("Work/Study", "4h 15m", 0.65f, Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.height(16.dp))
                        DistributionItem("Breaks", "1h 30m", 0.23f, Color(0xFF10B981))
                        Spacer(modifier = Modifier.height(16.dp))
                        DistributionItem("Distractions", "45m", 0.12f, Color(0xFFEF4444))
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Surface(color = Color(0xFFF0FDF4), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(text = "TOTAL ACTIVE TIME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                    Text(text = "6.5 hours", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14532D))
                                }
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                // 6. Optimization Suggestions
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Optimization Suggestions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1F2937))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "✨", fontSize = 16.sp)
                    }
                    SuggestionCard(title = "Increase Break Frequency", sub = "Your focus drops significantly after 90 minutes. Try the Pomodoro technique (25m work, 5m break).", tag = "HIGH IMPACT", tagColor = Color(0xFFEF4444), tagBg = Color(0xFFFEE2E2))
                    SuggestionCard(title = "Morning Peak alignment", sub = "You are most productive between 10AM - 11AM. Schedule your hardest tasks for this window.", tag = "MEDIUM IMPACT", tagColor = Color(0xFFF59E0B), tagBg = Color(0xFFFEF3C7))
                }

                // Footer Button
                Button(
                    onClick = { navController.navigate("burnout_risk") },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7))), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Text(text = "View Burnout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ChangeBox(label: String, value: String, color: Color, textColor: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = color) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(text = label, fontSize = 10.sp, color = textColor.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun InsightMiniCard(icon: ImageVector, value: String, label: String, sub: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Color(0xFFF5F3FF)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp)) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            Text(text = sub, fontSize = 10.sp, color = Color.Gray, lineHeight = 14.sp)
        }
    }
}

@Composable
fun DistributionItem(label: String, time: String, progress: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, fontSize = 13.sp, color = Color.Gray)
            Text(text = time, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = color, trackColor = Color(0xFFF3F4F6), strokeCap = StrokeCap.Round)
    }
}

@Composable
fun SuggestionCard(title: String, sub: String, tag: String, tagColor: Color, tagBg: Color) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937), modifier = Modifier.weight(1f))
                Surface(color = tagBg, shape = RoundedCornerShape(4.dp)) {
                    Text(text = tag, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = tagColor)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = sub, fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp)
        }
    }
}

@Composable
fun ProductivityLegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun MultiLineChart() {
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val width = size.width
        val height = size.height
        val days = 7
        val spacing = width / (days - 1)
        
        // Mock data points
        val overall = listOf(0.4f, 0.35f, 0.55f, 0.5f, 0.65f, 0.75f, 0.68f)
        val focus = listOf(0.3f, 0.25f, 0.45f, 0.4f, 0.55f, 0.65f, 0.6f)
        val efficiency = listOf(0.5f, 0.45f, 0.65f, 0.6f, 0.75f, 0.85f, 0.8f)

        fun drawLine(points: List<Float>, color: Color, dashed: Boolean = false) {
            val path = Path()
            points.forEachIndexed { index, value ->
                val x = index * spacing
                val y = height - (value * height)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color, style = if (dashed) Stroke(2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)) else Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            points.forEachIndexed { index, value ->
                drawCircle(color, 4.dp.toPx(), Offset(index * spacing, height - (value * height)))
                drawCircle(Color.White, 2.dp.toPx(), Offset(index * spacing, height - (value * height)))
            }
        }

        drawLine(overall, Color(0xFF10B981))
        drawLine(focus, Color(0xFF3B82F6), true)
        drawLine(efficiency, Color(0xFF8B5CF6))
    }
}

@Composable
fun PeakAreaChart() {
    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        val width = size.width
        val height = size.height
        
        val points = listOf(0.2f, 0.7f, 0.4f, 0.3f, 0.5f, 0.2f)
        val spacing = width / (points.size - 1)
        
        val path = Path().apply {
            moveTo(0f, height)
            points.forEachIndexed { index, value ->
                lineTo(index * spacing, height - (value * height))
            }
            lineTo(width, height)
            close()
        }
        
        drawPath(path, Brush.verticalGradient(listOf(Color(0xFF10B981).copy(alpha = 0.3f), Color.Transparent)))
        
        val linePath = Path().apply {
            points.forEachIndexed { index, value ->
                if (index == 0) moveTo(0f, height - (value * height))
                else lineTo(index * spacing, height - (value * height))
            }
        }
        drawPath(linePath, Color(0xFF10B981), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}
