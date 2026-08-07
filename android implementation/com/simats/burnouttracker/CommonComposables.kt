package com.simats.burnouttracker

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.utils.PlatformType
import com.simats.burnouttracker.utils.getPlatform
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.testTag
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class BarLineDataPoint(val label: String, val barValue: Float, val lineValue: Float, val barColor: Color)

@Composable
fun BarLineChart(data: List<BarLineDataPoint>) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF9CA3AF), fontSize = 11.sp)
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 10.dp)) {
        val width = size.width
        val height = size.height - 40.dp.toPx()
        val barWidth = 48.dp.toPx()
        val spacing = (width - (barWidth * data.size)) / (data.size + 1)
        val linePoints = mutableListOf<Offset>()
        data.forEachIndexed { index, item ->
            val x = spacing + index * (barWidth + spacing)
            val barHeight = item.barValue * height
            drawRoundRect(
                color = item.barColor,
                topLeft = Offset(x, height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
            )
            val textLayoutResult = textMeasurer.measure(item.label, textStyle)
            drawText(textLayoutResult, topLeft = Offset(x + barWidth / 2 - textLayoutResult.size.width / 2, height + 12.dp.toPx()))
            val lineY = height - (item.lineValue * height)
            linePoints.add(Offset(x + barWidth / 2, lineY))
        }
        if (linePoints.size > 1) {
            val path = Path()
            path.moveTo(linePoints[0].x, linePoints[0].y)
            for (i in 1 until linePoints.size) { path.lineTo(linePoints[i].x, linePoints[i].y) }
            drawPath(path = path, color = Color(0xFF6366F1), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            linePoints.forEach { point ->
                drawCircle(Color.White, radius = 6.dp.toPx(), center = point)
                drawCircle(Color(0xFF6366F1), radius = 6.dp.toPx(), center = point, style = Stroke(width = 2.dp.toPx()))
                drawCircle(Color(0xFF6366F1), radius = 2.5.dp.toPx(), center = point)
            }
        }
    }
}

@Composable
fun RadarChart(
    modifier: Modifier = Modifier,
    labels: List<String>,
    dataPoints: List<Float>,
    color: Color = Color(0xFFF97316),
    fillColor: Brush = Brush.linearGradient(listOf(Color(0xFFF97316).copy(alpha = 0.2f), Color(0xFFF97316).copy(alpha = 0.2f)))
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = androidx.compose.ui.text.TextStyle(
        color = Color(0xFF6B7280),
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2 * 0.7f
        val numPoints = labels.size
        val angleStep = (2 * PI / numPoints).toFloat()

        // Draw background polygons (hexagons in this case)
        for (i in 1..4) {
            val r = radius * (i / 4f)
            val path = Path()
            for (j in 0 until numPoints) {
                val angle = j * angleStep - (PI / 2).toFloat()
                val x = centerX + r * cos(angle)
                val y = centerY + r * sin(angle)
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, Color(0xFFE5E7EB), style = Stroke(width = 1.dp.toPx()))
        }

        for (i in 0 until numPoints) {
            val angle = i * angleStep - (PI / 2).toFloat()
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            drawLine(Color(0xFFE5E7EB), Offset(centerX, centerY), Offset(x, y))
            
            // Draw Labels
            val labelRadius = radius * 1.25f
            val labelX = centerX + labelRadius * cos(angle)
            val labelY = centerY + labelRadius * sin(angle)
            
            val textLayoutResult = textMeasurer.measure(labels[i], textStyle)
            drawText(
                textLayoutResult,
                topLeft = Offset(
                    labelX - textLayoutResult.size.width / 2,
                    labelY - textLayoutResult.size.height / 2
                )
            )
        }

        // Draw data area
        val dataPath = Path()
        val pointOffsets = mutableListOf<Offset>()
        for (i in 0 until numPoints) {
            val angle = i * angleStep - (PI / 2).toFloat()
            val r = radius * dataPoints[i]
            val x = centerX + r * cos(angle)
            val y = centerY + r * sin(angle)
            pointOffsets.add(Offset(x, y))
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        drawPath(dataPath, fillColor)
        drawPath(dataPath, color, style = Stroke(width = 2.dp.toPx()))
        
        for (point in pointOffsets) {
            drawCircle(color, radius = 4.dp.toPx(), center = point)
            drawCircle(Color.White, radius = 2.dp.toPx(), center = point)
        }
    }
}

@Composable
fun CategoryItem(icon: ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(14.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false,
    selectedColor: Color = Color(0xFF9333EA),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color = if (isSelected) selectedColor else Color(0xFF9CA3AF)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .width(64.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = color,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun AppBottomNavigation(
    navController: NavController,
    currentRoute: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
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
                icon = Icons.Default.GridView,
                label = "Home",
                isSelected = currentRoute == "dashboard",
                modifier = Modifier.testTag("navHome"),
                onClick = { if (currentRoute != "dashboard") navController.navigate("dashboard") }
            )
            BottomNavItem(
                icon = Icons.Default.Timer,
                label = "Tracker",
                isSelected = currentRoute == "tracker",
                modifier = Modifier.testTag("navTracker"),
                onClick = { if (currentRoute != "tracker") navController.navigate("study_tracker") }
            )
            BottomNavItem(
                icon = Icons.Default.BarChart,
                label = "Stats",
                isSelected = currentRoute == "burnout_risk",
                modifier = Modifier.testTag("navStats"),
                onClick = { if (currentRoute != "burnout_risk") navController.navigate("burnout_risk") }
            )
            BottomNavItem(
                icon = Icons.Default.Person,
                label = "Profile",
                isSelected = currentRoute == "settings",
                modifier = Modifier.testTag("navProfile"),
                onClick = { if (currentRoute != "settings") navController.navigate("settings") }
            )
        }
    }
}

@Composable
fun SleepStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    iconBgColor: Color,
    iconTint: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = iconBgColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        }
    }
}
