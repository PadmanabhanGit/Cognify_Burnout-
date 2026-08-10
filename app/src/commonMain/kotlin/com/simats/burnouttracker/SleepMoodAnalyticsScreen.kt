package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.data.rememberSleepRepository
import com.simats.burnouttracker.ui.SleepViewModel

/**
 * Real data available here: SleepDao.getRecentSessions() returns the most
 * recent real Room `sleep_sessions` rows, `ORDER BY sleepStart DESC LIMIT 7`
 * — the same query SleepMoodDashboardScreen's session list is built from.
 * There is no 30-day (or any longer) query anywhere in the app, and adding
 * one is out of scope for this pass (would mean touching SleepDao.kt beyond
 * the minimum), so this screen only ever claims what it can actually prove:
 * "last N real nights," where N is however many real sessions exist, up to
 * that 7-row cap — never a fixed "7-Day"/"30-Day" label independent of what
 * was actually detected.
 */
@Composable
fun SleepMoodAnalyticsScreen(navController: NavController) {
    val repository = rememberSleepRepository()
    val viewModel = remember { SleepViewModel(repository) }
    val sessions by viewModel.sessions.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshData() }

    // Chronological (oldest -> newest) for left-to-right charting; DAO already
    // returns newest-first.
    val chronological = remember(sessions) { sessions.reversed() }
    val hasAnySessions = sessions.isNotEmpty()
    val hasTrendHistory = sessions.size >= 2

    val averageQuality = if (hasAnySessions) {
        sessions.map { it.sleepQuality }.average().let { kotlin.math.round(it).toInt() }
    } else null

    val headerGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF4F46E5), Color(0xFF9333EA))
    )

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColors.background)
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
                // Average Quality — a single real stat, honestly labeled with the
                // actual number of real detected nights it covers. No "30-Day Avg":
                // no 30-day (or any multi-week) data source exists anywhere in this
                // app, so that label would always have been false precision.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (averageQuality == null) {
                            Text("Average Quality", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("No detected sleep sessions yet", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        } else {
                            Text("Average Quality — Last ${sessions.size} Night${if (sessions.size == 1) "" else "s"}", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$averageQuality%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                        }
                    }
                }

                // Trend Graph — real per-session dates and real sleepQuality only.
                // Below a minimum of 2 real sessions, a "trend" line is either
                // impossible (0 points) or meaningless/misleading (a single dot
                // drawn as a line) so we say so instead of drawing it.
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
                                Icon(Icons.Default.ShowChart, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Quality Trend", fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
                            }
                            if (hasTrendHistory) {
                                Surface(color = ThemeColors.background, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = "Last ${chronological.size} nights",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!hasTrendHistory) {
                            Text(
                                text = if (hasAnySessions)
                                    "Only one detected sleep session so far — a trend needs at least two real nights."
                                else
                                    "No detected sleep sessions yet. A trend will appear once the automatic sleep monitor has recorded a few nights.",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        } else {
                            Box(modifier = Modifier.height(140.dp).fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                    repeat(4) {
                                        HorizontalDivider(color = ThemeColors.background, thickness = 1.dp)
                                    }
                                }
                                SleepMoodLineChart(
                                    dataPoints = chronological.map { it.sleepQuality / 100f },
                                    color = Color(0xFF4F46E5),
                                    showFill = true
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                // Real dates for the real points actually plotted above
                                // — never a fixed Mon..Sun row, which would silently
                                // imply a full unbroken week regardless of gaps.
                                chronological.forEach { session ->
                                    Text(text = shortDateLabel(session.date), fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // "Top Disturbing Factors" removed. It previously showed three
                // hardcoded rows ("Night Social Media / Heavy Impact", etc.)
                // unconditionally, regardless of any real data. The engine
                // computes real per-category usage minutes while scoring a night
                // (SleepMonitoringEngine.kt, scoring section) but never persists
                // them anywhere, so there is currently no real data source this
                // screen could use to replace that section honestly. Removing it
                // rather than showing an empty/fake placeholder, per the same
                // rule applied to the rest of Sleep & Mood this pass.
            }
        }
    }
}

/** "2026-08-10" -> "Aug 10". Falls back to the raw string if it doesn't match. */
private fun shortDateLabel(isoDate: String): String {
    val parts = isoDate.split("-")
    if (parts.size != 3) return isoDate
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val monthIndex = parts[1].toIntOrNull()?.minus(1) ?: return isoDate
    if (monthIndex !in months.indices) return isoDate
    return "${months[monthIndex]} ${parts[2].toIntOrNull() ?: parts[2]}"
}
