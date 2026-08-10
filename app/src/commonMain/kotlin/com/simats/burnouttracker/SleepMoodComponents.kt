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

// RecentSleepLogsCard and SleepMoodTrendCard removed.
//
// Both were confirmed to have zero callers anywhere in the codebase. They were
// also the last remaining sources of fabricated presentation in Sleep & Mood:
//  - RecentSleepLogsCard was the sole navigate() caller for the retired
//    sleep_mood_details route.
//  - SleepMoodTrendCard hardcoded a "Last 7 Days" badge and a fixed
//    Mon..Sun axis, both independent of whatever data was passed in.
//
// SleepLogItem was used only by RecentSleepLogsCard and is removed with it.
// SleepMoodLineChart is KEPT — it has a live caller in SleepMoodAnalyticsScreen.

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
            // "Health" is the Sleep & Mood HOME entry (sleep_mood), matching the
            // Dashboard -> Home -> Analysis -> History flow. It previously jumped
            // straight to sleep_mood_dashboard, which made Analysis look like a
            // second, competing home page.
            BottomNavItem(
                icon = Icons.Default.MonitorHeart,
                label = "Health",
                isSelected = currentRoute == "sleep_mood" || currentRoute == "sleep_mood_dashboard",
                selectedColor = Color(0xFF4F46E5),
                onClick = { if (currentRoute != "sleep_mood") navController.navigate("sleep_mood") }
            )
            // Previously a dead button with an empty onClick. Points at the real
            // Sleep History screen — no new route, no new screen.
            BottomNavItem(
                icon = Icons.Default.QueryStats,
                label = "Trends",
                isSelected = currentRoute == "sleep_mood_analytics",
                selectedColor = Color(0xFF4F46E5),
                onClick = { if (currentRoute != "sleep_mood_analytics") navController.navigate("sleep_mood_analytics") }
            )
            BottomNavItem(
                icon = Icons.Default.Person, 
                label = "Profile", 
                onClick = { navController.navigate("settings") }
            )
        }
    }
}
