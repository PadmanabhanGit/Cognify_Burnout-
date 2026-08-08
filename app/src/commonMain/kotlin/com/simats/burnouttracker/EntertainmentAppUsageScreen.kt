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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.data.api.ApiClient
import com.simats.burnouttracker.data.models.*
import com.simats.burnouttracker.utils.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EntertainmentAppUsageScreen(navController: NavController) {
    val screenBgColor = ThemeColors.background
    val headerGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
    )

    val usageHelper = rememberUsageStatsHelper()
    val predictor = rememberBurnoutPredictor()
    val scope = rememberCoroutineScope()

    // Real-time update logic
    LaunchedEffect(Unit) {
        while(true) {
            if (usageHelper.hasUsageStatsPermission()) {
                val realFeatures = usageHelper.fetchDailyUsage()
                AppData.currentFeatures = realFeatures
                AppData.predictedScore = predictor.predict(realFeatures)
                
                // Sync to backend
                try {
                    AppData.isSyncing = true
                    val usageItems = realFeatures.topApps.map { appUsage: DetailedAppUsage ->
                        UsageItemRequest(
                            packageName = appUsage.packageName,
                            category = appUsage.category,
                            duration = (appUsage.hours * 60).toLong()
                        )
                    }
                    val response = ApiClient.syncUsageData(UsageSyncRequest(usageData = usageItems))
                    AppData.lastSyncFailed = !response.success
                } catch (e: Exception) {
                    AppData.lastSyncFailed = true
                } finally {
                    AppData.isSyncing = false
                }
            }
            delay(10000) // Update every 10 seconds for near real-time sync
        }
    }

    // Using central dynamic data
    val features = AppData.currentFeatures
    val riskScore = AppData.predictedScore

    Scaffold(
        containerColor = screenBgColor,
        bottomBar = { AppBottomNavigation(navController, "entertainment_usage") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header (Image 1 style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(headerGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 40.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
                    }
                }
                Column(modifier = Modifier.padding(top = 60.dp)) {
                    Text(
                        text = "App Usage",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Track your leisure time and its impact",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-20).dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Permission Check UI
                if (!usageHelper.hasUsageStatsPermission()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Usage Access Required", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                            Text("We need permission to track your app usage for accurate burnout analysis.", fontSize = 12.sp, textAlign = TextAlign.Center, color = Color(0xFF991B1B))
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { usageHelper.openUsageStatsSettings() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Grant Permission", color = Color.White)
                            }
                        }
                    }
                }

                // 1. Daily Entertainment Usage Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Daily Usage", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(color = ThemeColors.background, shape = RoundedCornerShape(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        if (AppData.isSyncing) {
                                            CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.dp, color = ThemeColors.textSecondary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(text = "Today", fontSize = 12.sp, color = ThemeColors.textSecondary)
                                    }
                                }
                                if (AppData.lastUpdatedTime.isNotEmpty()) {
                                    Text(
                                        text = if (AppData.lastSyncFailed) "Sync Failed" else "Updated: ${AppData.lastUpdatedTime}", 
                                        fontSize = 10.sp, 
                                        color = if (AppData.lastSyncFailed) Color.Red else Color.Gray
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        UsageItem(
                            label = "Social Media",
                            duration = formatHours(features.socialHours),
                            progress = features.socialHours / 12f,
                            color = Color(0xFFF43F5E),
                            icon = Icons.Default.Groups
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        UsageItem(
                            label = "Gaming",
                            duration = formatHours(features.gamingHours),
                            progress = features.gamingHours / 10f,
                            color = Color(0xFFF59E0B),
                            icon = Icons.Default.SportsEsports
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        UsageItem(
                            label = "Streaming",
                            duration = formatHours(features.streamingHours),
                            progress = features.streamingHours / 10f,
                            color = Color(0xFF3B82F6),
                            icon = Icons.Default.Tv
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        UsageItem(
                            label = "Productivity",
                            duration = formatHours(features.productivityHours),
                            progress = features.productivityHours / 12f,
                            color = Color(0xFF10B981),
                            icon = Icons.Default.MenuBook
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = ThemeColors.background)
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Total App Usage", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textSecondary)
                            Text(text = formatHours(features.totalScreenTime), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111827))
                        }
                    }
                }

                // 1.5 Top Used Apps Today
                if (features.topApps.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(text = "Top Used Apps Today", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            features.topApps.forEach { app ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(10.dp),
                                        shape = CircleShape,
                                        color = app.color
                                    ) {}
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = app.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                                        Text(text = app.category, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text(text = formatHours(app.hours), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111827))
                                }
                            }
                        }
                    }
                }

                // 2. Burnout Risk Visualization Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Burnout Risk Visualization", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
                        Text(text = "Correlation between app type and burnout score", fontSize = 12.sp, color = ThemeColors.textSecondary)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(12.dp, 2.dp).background(Color(0xFF6366F1)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Burnout Risk Score", fontSize = 10.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Dynamic Chart
                        BarLineChart(
                            listOf(
                                BarLineDataPoint("Entertain", ((features.socialHours + features.gamingHours + features.streamingHours) / 10f).coerceIn(0f, 1f), riskScore / 100f, Color(0xFFF87171)),
                                BarLineDataPoint("Gaming", (features.gamingHours / 10f).coerceIn(0f, 1f), (riskScore * 0.8f / 100f).coerceIn(0f, 1f), Color(0xFFFB923C)),
                                BarLineDataPoint("Productivity", (features.productivityHours / 10f).coerceIn(0f, 1f), (riskScore * 0.4f / 100f).coerceIn(0f, 1f), Color(0xFF4ADE80))
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Insights
                        val highEntertainment = (features.socialHours + features.gamingHours + features.streamingHours) > 4f
                        val lowProductivity = features.productivityHours < 2f

                        if (highEntertainment) {
                            Surface(color = Color(0xFFFFF1F2), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = buildAnnotatedString {
                                            append("Your entertainment usage is high. This can reduce focus by ")
                                            pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFE11D48)))
                                            append("${(riskScore * 0.4).toInt()}%")
                                            pop()
                                            append(".")
                                        },
                                        fontSize = 11.sp, color = Color(0xFF9F1239)
                                    )
                                }
                            }
                        } else {
                            Surface(color = Color(0xFFF0FDF4), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = "Great job! Your entertainment usage is within healthy limits.", fontSize = 11.sp, color = Color(0xFF166534))
                                }
                            }
                        }

                        if (lowProductivity && riskScore > 50) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = "Try to balance leisure with at least 30 mins of focused study.", fontSize = 11.sp, color = Color(0xFF92400E))
                                }
                            }
                        }
                    }
                }

                // 3. Focus & Concentration Analysis Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    val wellbeing = WellbeingGenerator.generate(riskScore, InsightGenerator.generate(features, riskScore))
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Focus & Concentration Analysis", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary, modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        RadarChart(
                            modifier = Modifier.size(240.dp),
                            labels = listOf("Focus", "Productivity", "Entertain", "Sleep", "Gaming", "Mood"),
                            dataPoints = listOf(
                                wellbeing.focus / 100f,
                                wellbeing.studyLoad / 100f,
                                ((features.socialHours + features.streamingHours) / 10f).coerceIn(0f, 1f),
                                wellbeing.sleep / 100f,
                                (features.gamingHours / 5f).coerceIn(0f, 1f),
                                wellbeing.mood / 100f
                            ),
                            color = Color(0xFF8B5CF6),
                            fillColor = Brush.radialGradient(listOf(Color(0xFFC4B5FD).copy(alpha = 0.5f), Color(0xFF8B5CF6).copy(alpha = 0.2f)))
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Surface(color = ThemeColors.background, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(text = "High entertainment usage correlates with lower focus scores.", fontSize = 12.sp, color = ThemeColors.textSecondary, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun UsageItem(label: String, duration: String, progress: Float, color: Color, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(36.dp), shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.1f)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp)) }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF374151))
                Text(text = duration, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF111827))
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp), color = color, trackColor = ThemeColors.background, strokeCap = StrokeCap.Round)
        }
    }
}

@Composable
fun RecommendationItem(title: String, subtitle: String, icon: ImageVector) {
    Surface(
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

private fun formatHours(hours: Float): String {
    val totalSecs = (hours * 3600).toLong()
    if (totalSecs <= 0) return "0s"
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    
    val parts = mutableListOf<String>()
    if (h > 0) parts.add("${h}h")
    if (m > 0) parts.add("${m}m")
    if (s > 0) parts.add("${s}s")
    
    return if (parts.isNotEmpty()) parts.joinToString(" ") else "0s"
}
