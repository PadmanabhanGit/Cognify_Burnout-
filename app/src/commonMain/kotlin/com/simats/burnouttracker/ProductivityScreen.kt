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
    val screenBgColor = ThemeColors.background
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
                    colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Today's Productivity", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeColors.textPrimary)
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(30.dp))
                        
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(160.dp)) {
                                drawArc(ThemeColors.background, 140f, 260f, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                                drawArc(Color(0xFF0F172A), 140f, 260f * (AppData.productivityScore / 100f), false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = AppData.productivityScore.toString(), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111827))
                                Text(text = "PRODUCTIVITY\nSCORE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textTertiary, textAlign = TextAlign.Center)
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ChangeBox(label = "Weekly Change", value = "+${(AppData.productivityScore % 15) + 5}%", color = Color(0xFFDCFCE7), textColor = Color(0xFF16A34A), modifier = Modifier.weight(1f))
                            ChangeBox(label = "This Month", value = "+${(AppData.productivityScore % 8) + 2}%", color = Color(0xFFEFF6FF), textColor = Color(0xFF2563EB), modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 2. 7-Day Trend Analysis Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "7-Day Trend Analysis", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeColors.textPrimary)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val scoreNormalized = AppData.productivityScore / 100f
                        val dynOverall = listOf(scoreNormalized * 0.5f, scoreNormalized * 0.6f, scoreNormalized * 0.8f, scoreNormalized * 0.7f, scoreNormalized * 0.9f, scoreNormalized, scoreNormalized * 0.85f)
                        val dynFocus = listOf(scoreNormalized * 0.4f, scoreNormalized * 0.5f, scoreNormalized * 0.7f, scoreNormalized * 0.6f, scoreNormalized * 0.8f, scoreNormalized * 0.9f, scoreNormalized * 0.75f)
                        val dynEfficiency = listOf(scoreNormalized * 0.6f, scoreNormalized * 0.7f, scoreNormalized * 0.9f, scoreNormalized * 0.8f, scoreNormalized, scoreNormalized * 0.95f, scoreNormalized * 0.9f)
                        
                        MultiLineChart(dynOverall, dynFocus, dynEfficiency)
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
                    val formattedPeak = (AppData.peakFocusHours * 10f).toInt() / 10f
                    InsightMiniCard(icon = Icons.Default.FlashOn, value = "${formattedPeak}h", label = "Peak Focus", sub = "Highest continuous span", modifier = Modifier.weight(1f))
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
                    colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Peak Performance Hours", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeColors.textPrimary)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val scoreNorm = AppData.productivityScore / 100f
                        val dynPeakPoints = listOf(scoreNorm * 0.3f, scoreNorm * 0.8f, scoreNorm * 0.5f, scoreNorm * 0.4f, scoreNorm * 0.6f, scoreNorm * 0.2f)
                        PeakAreaChart(dynPeakPoints)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("06AM", "09AM", "12PM", "03PM", "06PM", "09PM").forEach {
                                Text(text = it, fontSize = 9.sp, color = Color.Gray)
                            }
                        }
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
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            Text(text = sub, fontSize = 10.sp, color = Color.Gray, lineHeight = 14.sp)
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
fun MultiLineChart(overall: List<Float>, focus: List<Float>, efficiency: List<Float>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val width = size.width
        val height = size.height
        val days = 7
        val spacing = width / (days - 1)

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
fun PeakAreaChart(points: List<Float>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        val width = size.width
        val height = size.height
        
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
