package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.simats.burnouttracker.data.models.ProductivityLog
import com.simats.burnouttracker.data.models.ProductivityLogRequest
import com.simats.burnouttracker.data.models.ProductivityWeeklyDay
import com.simats.burnouttracker.utils.AppData

/** Distinguishes "haven't fetched yet", "fetched, backend is canonical", and
 *  "the request itself failed" — these must never be collapsed into the same
 *  UI (a failed request is not the same thing as a real score of 0 or no
 *  record for today). */
private enum class LoadState { LOADING, LOADED, ERROR }

@Composable
fun ProductivityScreen(navController: NavController) {
    val greenGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF10B981), Color(0xFF059669))
    )
    val screenBgColor = ThemeColors.background

    var loadState by remember { mutableStateOf(LoadState.LOADING) }
    var todayLog by remember { mutableStateOf<ProductivityLog?>(null) }
    var weeklyDays by remember { mutableStateOf<List<ProductivityWeeklyDay>>(emptyList()) }
    var weeklyLoaded by remember { mutableStateOf(false) }

    // Single fetch on mount — no polling.
    //
    // Checks for today's canonical record FIRST. If one already exists,
    // it is displayed as-is and NOT re-posted: AppData.productivityScore is a
    // shared value that Android Dashboard's local predictor loop also writes
    // to whenever the Dashboard screen is open, so blindly POSTing it here on
    // every visit could silently overwrite an already-correct persisted score
    // with an unrelated Dashboard-driven recomputation. Only when no record
    // exists yet for today does this screen establish one (existing behavior,
    // preserved for the first-visit-of-the-day case).
    LaunchedEffect(Unit) {
        val existing = ApiClient.getProductivityToday()
        if (existing.success && existing.log != null) {
            todayLog = existing.log
            loadState = LoadState.LOADED
        } else if (AppData.hasData) {
            // A legitimate local predictor candidate exists. AppData.hasData is
            // set (DashboardScreen.kt) only after ProductivityPredictor.calculate(...)
            // has actually run this session inside the Usage Stats permission
            // branch — it is never set merely because Dashboard opened, so it
            // reliably distinguishes a real candidate from AppData.productivityScore's
            // untouched default. Safe to seed today's first record with it.
            try {
                ApiClient.logProductivity(
                    ProductivityLogRequest(
                        productivityScore = AppData.productivityScore,
                        focusHours = AppData.peakFocusHours.toDouble()
                    )
                )
            } catch (e: Exception) {
                // POST failure doesn't block reading back whatever is already persisted.
            }

            val afterPost = ApiClient.getProductivityToday()
            if (afterPost.success) {
                todayLog = afterPost.log
                loadState = LoadState.LOADED
            } else {
                loadState = LoadState.ERROR
            }
        } else {
            // No canonical record for today, and no legitimate candidate was
            // ever computed this session (e.g. Usage Stats permission isn't
            // granted, so DashboardScreen's predictor never ran). Do NOT post
            // AppData.productivityScore's untouched default (0) as if it were a
            // real score. Leave todayLog null so the existing "NO DATA TODAY
            // YET" state below renders honestly instead.
            loadState = LoadState.LOADED
        }

        val weeklyResponse = ApiClient.getProductivityWeekly()
        if (weeklyResponse.success) {
            weeklyDays = weeklyResponse.days
        }
        weeklyLoaded = true
    }

    val availableDays = weeklyDays.filter { it.available && it.productivityScore != null }

    // Genuine day-over-day comparison computed only from two real persisted
    // values (today's record and yesterday's /weekly entry) — never derived
    // from the score itself via an arbitrary formula.
    val todayIndex = todayLog?.date?.let { d -> weeklyDays.indexOfFirst { it.date == d } } ?: -1
    val yesterdayScore = if (todayIndex > 0) weeklyDays[todayIndex - 1].takeIf { it.available }?.productivityScore else null
    val todayScoreForCompare = todayLog?.productivityScore
    val dayOverDayChange: Int? = if (todayScoreForCompare != null && yesterdayScore != null) {
        todayScoreForCompare - yesterdayScore
    } else null

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
                // 1. Today's Productivity Card — driven by the persisted backend
                // record (todayLog), not the live in-memory AppData value.
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

                        val score = todayLog?.productivityScore
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(160.dp)) {
                                drawArc(ThemeColors.background, 140f, 260f, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                                if (loadState == LoadState.LOADED && score != null) {
                                    drawArc(Color(0xFF0F172A), 140f, 260f * (score / 100f), false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                when {
                                    loadState == LoadState.LOADING -> {
                                        Text(text = "…", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = ThemeColors.textTertiary)
                                        Text(text = "LOADING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textTertiary, textAlign = TextAlign.Center)
                                    }
                                    loadState == LoadState.ERROR -> {
                                        Text(text = "--", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = ThemeColors.textTertiary)
                                        Text(text = "UNAVAILABLE\n(request failed)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textTertiary, textAlign = TextAlign.Center)
                                    }
                                    score == null -> {
                                        Text(text = "--", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = ThemeColors.textTertiary)
                                        Text(text = "NO DATA\nTODAY YET", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textTertiary, textAlign = TextAlign.Center)
                                    }
                                    else -> {
                                        Text(text = score.toString(), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111827))
                                        Text(text = "PRODUCTIVITY\nSCORE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textTertiary, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val changeText = dayOverDayChange?.let { d -> if (d >= 0) "+$d" else "$d" } ?: "Insufficient data"
                            ChangeBox(label = "vs Yesterday", value = changeText, color = Color(0xFFDCFCE7), textColor = Color(0xFF16A34A), modifier = Modifier.weight(1f))
                            ChangeBox(label = "This Month", value = "Insufficient data", color = Color(0xFFEFF6FF), textColor = Color(0xFF2563EB), modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 2. 7-Day Trend — Monday..Sunday from the backend's real /weekly
                // days[]. Missing days render as gaps, never as a measured zero.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "7-Day Trend", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeColors.textPrimary)
                        Spacer(modifier = Modifier.height(24.dp))

                        if (!weeklyLoaded) {
                            Text(text = "Loading…", fontSize = 12.sp, color = ThemeColors.textTertiary)
                        } else if (availableDays.size < 2) {
                            Text(
                                text = "Insufficient history — need at least 2 days of real productivity data this week to show a trend.",
                                fontSize = 12.sp,
                                color = ThemeColors.textTertiary
                            )
                        } else {
                            WeeklyTrendChart(weeklyDays)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                                    Text(text = it, fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // 3. Key Insights — only the one metric with a real persisted
                // source. Goal Hit / Start Time / Ranking were removed: no
                // legitimate backend source exists for any of them (see the
                // Productivity audit). Label is honest about what focusHours
                // actually is: a proxy (avg session length x2), not a measured
                // "peak" span.
                val focusHours = todayLog?.focusHours
                InsightMiniCard(
                    icon = Icons.Default.FlashOn,
                    value = if (loadState == LoadState.LOADED && focusHours != null) "${(focusHours * 10).toInt() / 10.0}h" else "--",
                    label = "Avg Focus Span",
                    sub = "Proxy from average session length, not a measured peak",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ChangeBox(label: String, value: String, color: Color, textColor: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = color) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontSize = if (value == "Insufficient data") 11.sp else 18.sp, fontWeight = FontWeight.Bold, color = textColor)
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

/** Draws only the real, available days from the backend's /weekly response —
 *  connects a line between consecutive available days and leaves a gap where
 *  a day is unavailable, rather than interpolating or treating it as zero. */
@Composable
fun WeeklyTrendChart(days: List<ProductivityWeeklyDay>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val width = size.width
        val height = size.height
        val n = days.size
        if (n < 2) return@Canvas
        val spacing = width / (n - 1)

        fun yFor(score: Int) = height - ((score / 100f) * height)

        var previousAvailableIndex: Int? = null
        days.forEachIndexed { index, day ->
            val x = index * spacing
            if (day.available && day.productivityScore != null) {
                val y = yFor(day.productivityScore)
                if (previousAvailableIndex != null && previousAvailableIndex == index - 1) {
                    val prevDay = days[previousAvailableIndex!!]
                    val prevScore = prevDay.productivityScore
                    if (prevScore != null) {
                        val prevX = previousAvailableIndex!! * spacing
                        val prevY = yFor(prevScore)
                        drawLine(Color(0xFF10B981), Offset(prevX, prevY), Offset(x, y), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
                drawCircle(Color(0xFF10B981), 4.dp.toPx(), Offset(x, y))
                drawCircle(Color.White, 2.dp.toPx(), Offset(x, y))
                previousAvailableIndex = index
            }
        }
    }
}
