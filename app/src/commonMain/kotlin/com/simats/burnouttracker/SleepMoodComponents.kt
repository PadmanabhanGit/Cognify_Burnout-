package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun MoodItem(label: String, emoji: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (isSelected) Color(0xFFEEF2FF) else ThemeColors.background,
                    CircleShape
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFF6366F1) else ThemeColors.border,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 26.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) Color(0xFF6366F1) else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun RecentSleepLogsCard(navController: NavController? = null) {
    val logs = com.simats.burnouttracker.utils.AppData.sleepLogs
    Surface(
        modifier = Modifier.fillMaxWidth()
            .clickable { navController?.navigate("sleep_mood_details") },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Logs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColors.textPrimary
                )
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (logs.isEmpty()) {
                Text(
                    text = "No logs yet. Start tracking to see your trends!",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                )
            } else {
                logs.take(3).forEach { log ->
                    SleepLogItem(
                        date = log.date,
                        time = "${((log.hours * 10).toInt() / 10f)}h",
                        status = log.status,
                        emoji = log.moodEmoji,
                        statusColor = log.statusColor
                    )
                }
            }
        }
    }
}

@Composable
fun SleepMoodTrendCard(

    title: String,
    icon: ImageVector,
    color: Color,
    dataPoints: List<Float>,
    showFill: Boolean,
    markerValue: String? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
                }
                Surface(
                    color = ThemeColors.background,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Last 7 Days",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.height(140.dp).fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    repeat(4) {
                        HorizontalDivider(color = ThemeColors.background, thickness = 1.dp)
                    }
                }
                
                SleepMoodLineChart(dataPoints = dataPoints, color = color, showFill = showFill)
                
                if (markerValue != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter).offset(x = 10.dp, y = 10.dp),
                        color = Color(0xFF1E1B4B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = markerValue,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                    Text(text = day, fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SleepMoodLineChart(dataPoints: List<Float>, color: Color, showFill: Boolean) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)
    )
    
    Canvas(
        modifier = Modifier.fillMaxSize().padding(vertical = 20.dp)
    ) {
        val width = size.width
        val height = size.height
        val spacing = width / (dataPoints.size - 1)
        
        if (showFill) {
            val fillPath = Path().apply {
                moveTo(0f, height)
                dataPoints.forEachIndexed { index, value ->
                    lineTo(index * spacing, height - (value * height))
                }
                lineTo(width, height)
                close()
            }
            drawPath(fillPath, brush = gradientBrush)
        }
        
        val strokePath = Path().apply {
            dataPoints.forEachIndexed { index, value ->
                val x = index * spacing
                val y = height - (value * height)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        
        drawPath(
            strokePath,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        
        dataPoints.forEachIndexed { index, value ->
            val x = index * spacing
            val y = height - (value * height)
            drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
            drawCircle(color, radius = 4.dp.toPx(), center = Offset(x, y), style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
fun SleepLogItem(date: String, time: String, status: String, emoji: String, statusColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = ThemeColors.background
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = date, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = time, fontSize = 13.sp, color = Color.Gray)
                    Text(text = " • ", color = Color.Gray)
                    Text(text = status, fontSize = 13.sp, color = statusColor, fontWeight = FontWeight.Medium)
                }
            }
            Icon(
                if (status == "Excellent") Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingFlat,
                contentDescription = null,
                tint = if (status == "Excellent") Color(0xFF22C55E) else Color(0xFFEAB308),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun TimelineItem(time: String, title: String, subtitle: String? = null, icon: ImageVector, color: Color, isLast: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.1f)
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(6.dp))
            }
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(ThemeColors.border))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(text = time, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = ThemeColors.background
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ThemeColors.textPrimary)
            Text(text = label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SleepMoodBottomNavigation(navController: NavController, currentRoute: String = "sleep_mood_dashboard") {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        color = Color.White,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Home, 
                label = "Home", 
                onClick = { navController.navigate("dashboard") }
            )
            BottomNavItem(
                icon = Icons.Default.MonitorHeart, 
                label = "Health", 
                isSelected = currentRoute == "sleep_mood_dashboard" || currentRoute == "sleep_mood" || currentRoute == "sleep_mood_logger",
                selectedColor = Color(0xFF4F46E5), 
                onClick = { if (currentRoute != "sleep_mood_dashboard") navController.navigate("sleep_mood_dashboard") }
            )
            BottomNavItem(
                icon = Icons.Default.QueryStats, 
                label = "Trends", 
                onClick = {}
            )
            BottomNavItem(
                icon = Icons.Default.Person, 
                label = "Profile", 
                onClick = { navController.navigate("settings") }
            )
        }
    }
}
