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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.utils.*

@Composable
fun BurnoutRiskScreen(navController: NavController) {
    val screenBgColor = ThemeColors.background
    val orangeGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFF7E3D), Color(0xFFF97316))
    )
    
    val predictor = rememberBurnoutPredictor()

    // Dynamic Data from Mock Store
    var riskScore by remember { mutableStateOf(AppData.predictedScore) }
    var riskLevel by remember { mutableStateOf(getRiskLevelText(riskScore)) }
    
    val features = AppData.currentFeatures

    LaunchedEffect(Unit) {
        val prediction = predictor.predict(features)
        if (prediction >= 0) {
            riskScore = prediction
            AppData.predictedScore = prediction
            riskLevel = getRiskLevelText(prediction)
        }
    }

    val insights = InsightGenerator.generate(features, riskScore, AppData.nightDataAvailable)
    val wellbeing = WellbeingGenerator.generate(riskScore, insights)
    val recommendations = RecommendationEngine.generate(riskScore)

    Scaffold(
        containerColor = screenBgColor,
        bottomBar = { AppBottomNavigation(navController, "burnout_risk") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section (Image 2 style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(orangeGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 40.dp, start = 20.dp, end = 20.dp)
            ) {
                Column {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Burnout Risk Analysis",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ML-driven mental fatigue prediction",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            // Main Content
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-30).dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Current Risk Level Card
                RiskGaugeCard(riskScore.toInt(), riskLevel)

                // 2. Warning Indicators Card
                WarningCard(riskScore)

                // 3. Contributing Factors Card
                ContributingFactorsCard(
                    listOf(
                        FactorItem("Study Hours", insights.studyLoad, Color(0xFFEF4444), Icons.AutoMirrored.Filled.MenuBook),
                        FactorItem("Sleep Quality", insights.sleepQuality, Color(0xFF3B82F6), Icons.Default.Bedtime),
                        FactorItem("Stress Level", insights.stressLevel, Color(0xFFEF4444), Icons.Default.Favorite),
                        FactorItem("Recovery Time", insights.recoveryTime, Color(0xFF3B82F6), Icons.Default.Restore)
                    )
                )

                // 4. Wellbeing Analysis Card
                WellbeingAnalysisCard(wellbeing)

                // 5. Action Plan Button
                Button(
                    onClick = { navController.navigate("generalized_action_plan") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF4F46E5), Color(0xFF9333EA))), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Generate Personalized Action Plan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun RiskGaugeCard(score: Int, level: String) {
    val dynamicColor = when {
        score > 75 -> Color(0xFFEF4444)
        score > 40 -> Color(0xFFF97316)
        else -> Color(0xFF10B981)
    }
    
    val bgDynamicColor = when {
        score > 75 -> Color(0xFFFEE2E2)
        score > 40 -> Color(0xFFFFF7ED)
        else -> Color(0xFFD1FAE5)
    }

    val assessmentText = burnoutAssessmentText(score.toFloat())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Current Risk Level", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeColors.textPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Warning, contentDescription = null, tint = dynamicColor, modifier = Modifier.size(18.dp))
                }
                Icon(Icons.Default.Info, contentDescription = null, tint = ThemeColors.textTertiary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(30.dp))
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    drawArc(color = ThemeColors.background, startAngle = 140f, sweepAngle = 260f, useCenter = false, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(color = dynamicColor, startAngle = 140f, sweepAngle = 260f * (score / 100f), useCenter = false, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$score%", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111827))
                    Text(text = level, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = dynamicColor)
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
            Surface(color = bgDynamicColor, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = dynamicColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Assessment", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = dynamicColor)
                        Text(text = assessmentText, fontSize = 12.sp, color = dynamicColor.copy(alpha = 0.9f))
                    }
                }
            }
        }
    }
}

@Composable
fun WarningCard(riskScore: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C))), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Warning Indicators", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            burnoutWarningIndicators(riskScore).forEach { WarningItem(it) }
        }
    }
}

@Composable
fun WarningItem(text: String) {
    Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ContributingFactorsCard(factors: List<FactorItem>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = ThemeColors.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Contributing Factors", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeColors.textPrimary)
            Spacer(modifier = Modifier.height(20.dp))
            factors.forEach { factor ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(factor.icon, contentDescription = null, tint = ThemeColors.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = factor.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ThemeColors.textSecondary)
                            Text(
                                text = factor.value?.let { "$it%" } ?: "No data yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (factor.value != null) factor.color else ThemeColors.textTertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { (factor.value ?: 0) / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp), color = if (factor.value != null) factor.color else ThemeColors.textTertiary.copy(alpha = 0.3f), trackColor = ThemeColors.background, strokeCap = StrokeCap.Round)
                    }
                }
            }
        }
    }
}

@Composable
fun WellbeingAnalysisCard(wellbeing: WellbeingMetrics) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = ThemeColors.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Wellbeing Analysis", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeColors.textPrimary)
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RadarChart(
                    modifier = Modifier.size(220.dp),
                    labels = listOf("Focus", "Stress", "Mood", "Energy", "Sleep", "Study"),
                    dataPoints = listOf(wellbeing.focus / 100f, wellbeing.stress / 100f, wellbeing.mood / 100f, wellbeing.energy / 100f, wellbeing.sleep / 100f, wellbeing.studyLoad / 100f),
                    color = Color(0xFFF97316),
                    fillColor = Brush.radialGradient(listOf(Color(0xFFF97316).copy(alpha = 0.3f), Color(0xFFF97316).copy(alpha = 0.1f)))
                )
            }
        }
    }
}

@Composable
fun RiskRecommendationsCard(recommendations: List<Recommendation>, riskScore: Float) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = ThemeColors.card), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "AI Recommendations", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeColors.textPrimary)
                Surface(color = Color(0xFFF3E8FF), shape = RoundedCornerShape(8.dp)) {
                    Text(text = "AI Generated", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, color = Color(0xFF9333EA), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            recommendations.forEach { rec ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val color = if (riskScore > 75) Color(0xFFEF4444) else Color(0xFFF59E0B)
                    val bg = if (riskScore > 75) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
                    Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = bg) {
                        Box(contentAlignment = Alignment.Center) { Icon(rec.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp)) }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = rec.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
                                Text(text = rec.subtitle, fontSize = 11.sp, color = Color.Gray)
                            }
                            Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
                                Text(text = if (riskScore > 75) "HIGH" else "MEDIUM", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getRiskLevelText(score: Float): String = when {
    score > 75 -> "HIGH"
    score > 40 -> "MODERATE"
    else -> "LOW"
}

/**
 * The assessment sentence rendered by RiskGaugeCard.
 *
 * Extracted verbatim from that composable so it has exactly ONE definition:
 * the Android UI renders it and DashboardScreen persists it. The rule itself is
 * unchanged — this is not a new algorithm.
 */
internal fun burnoutAssessmentText(score: Float): String = when {
    score > 75 -> "Your burnout risk is high. Immediate action and rest are recommended to prevent escalation."
    score > 40 -> "Your burnout risk is moderate. Pay attention to your stress levels and ensure you're taking enough breaks."
    else -> "Your burnout risk is low. You're maintaining a great balance! Keep up your healthy routines."
}

/**
 * The warning indicators rendered by WarningCard, in display order.
 * Extracted verbatim for the same reason as burnoutAssessmentText.
 */
internal fun burnoutWarningIndicators(score: Float): List<String> {
    val indicators = mutableListOf("Increased study hours (>15%)", "Sleep deficit detected")
    if (score > 60) indicators.add("Elevated stress levels")
    return indicators
}

data class FactorItem(val name: String, val value: Int?, val color: Color, val icon: ImageVector)
