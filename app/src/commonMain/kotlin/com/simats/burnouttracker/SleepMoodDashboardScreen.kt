package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.simats.burnouttracker.data.rememberSleepRepository
import com.simats.burnouttracker.ui.SleepViewModel
import com.simats.burnouttracker.utils.formatMinutes
import com.simats.burnouttracker.utils.formatTimestamp

/**
 * SLEEP ANALYSIS (route `sleep_mood_dashboard`) — the EXPLANATION layer.
 *
 * Sleep & Mood Home already answers "how was my sleep last night?" with a large
 * quality hero, four metric cards and a start/wake card. This screen must not
 * repeat that, so none of those elements appear here. Instead it explains what
 * the detected session actually CONTAINS:
 *
 *   1. Session Details — compact rows, including two facts Home cannot show:
 *      time in bed, and how much of it was spent awake. Both are plain
 *      arithmetic on sleepStart/sleepEnd/totalSleepMinutes.
 *   2. Sleep Timeline — the visual centrepiece, built from real WakeEvent rows.
 *   3. Sleep Quality — the engine's own score on a compact linear bar, never a
 *      second circular hero and never recalculated here.
 *   4. Disturbance — the engine's score plus the real per-category app activity
 *      recorded during the session, read from the `app_usage_logs` rows the
 *      engine itself wrote. No invented "disturbing factors".
 *
 * DATA SOURCES — every value below traces to one of:
 *   session.date, session.sleepStart, session.sleepEnd,
 *   session.totalSleepMinutes, session.awakeningCount, session.sleepQuality,
 *   session.disturbanceScore,
 *   wakeEvents[].timestamp / .appName / .category / .duration,
 *   usageLogs[].startTime / .duration / .appName / .category
 * Nothing else. No predictedScore, no AppData fallback, no hardcoded time,
 * percentage, duration or count.
 */
@Composable
fun SleepMoodDashboardScreen(navController: NavController) {
    val repository = rememberSleepRepository()
    val viewModel = remember { SleepViewModel(repository) }
    val sessions by viewModel.sessions.collectAsState()
    val latestSession = sessions.firstOrNull()
    val available = latestSession != null

    val wakeEvents by viewModel.selectedWakeEvents.collectAsState()

    // Real per-app records the engine persisted for this session. Already being
    // loaded by selectSession() below — this screen is simply the first thing
    // to actually read them.
    val usageLogs by viewModel.selectedSessionLogs.collectAsState()

    val headerGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF4F46E5), Color(0xFF9333EA))
    )

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    LaunchedEffect(latestSession) {
        latestSession?.let {
            viewModel.selectSession(it.id)
        }
    }

    Scaffold(
        bottomBar = { SleepMoodBottomNavigation(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColors.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Compact header — deliberately shorter than Home's, so entering this
            // screen reads as going deeper rather than sideways.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .padding(top = 36.dp, bottom = 28.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sleep Analysis",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Details from your detected sleep session",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Spacing only — no structural or data change.
            //
            // This Column previously used `.offset(y = (-16).dp)` to tuck its
            // content up under the purple header, a pattern copied from screens
            // whose first child is a white card. Here the first child is the
            // "Session Details" SectionLabel: plain dark-grey text, drawn after
            // the header and therefore ON TOP of it, which made the heading look
            // cut off / hidden against the purple.
            //
            // Replacing the negative offset with real top padding keeps the
            // heading fully inside the page body. Card styling, section order and
            // every displayed value are untouched.
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!available) {
                    // No detected session — one honest statement, no zero-valued
                    // metric cards, no previous session substituted in.
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Bedtime, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Sleep analysis unavailable",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "This page analyses one automatically detected sleep session. None has been recorded yet, so there is nothing to break down.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    val session = latestSession!!

                    // Time actually spanned by the session, and how much of it the
                    // engine did NOT count as sleep. Pure arithmetic on real
                    // fields — and the one thing Home structurally cannot show,
                    // since Home only reports the final totalSleepMinutes.
                    val timeInBedMinutes = ((session.sleepEnd - session.sleepStart) / 60000L).toInt()
                    val awakeMinutes = (timeInBedMinutes - session.totalSleepMinutes).coerceAtLeast(0)

                    // ── SECTION 1 — Session Details (compact rows) ───────────────
                    SectionLabel("Session Details")
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            DetailRow(Icons.Default.CalendarToday, "Night of", sessionDateLabel(session.date))
                            DetailRow(Icons.Default.Nightlight, "Detected sleep start", formatTimestamp(session.sleepStart))
                            DetailRow(Icons.Default.WbSunny, "Detected wake time", formatTimestamp(session.sleepEnd))
                            // Icons are restricted to ones already used elsewhere in
                            // this codebase, so no unverified materialIconsExtended
                            // symbol can break the build.
                            DetailRow(Icons.Default.HourglassEmpty, "Time in bed", formatMinutes(timeInBedMinutes))
                            DetailRow(Icons.Default.Bedtime, "Counted as sleep", formatMinutes(session.totalSleepMinutes))
                            DetailRow(Icons.Default.HourglassEmpty, "Awake during session", formatMinutes(awakeMinutes))
                            DetailRow(Icons.Default.NotificationsActive, "Awakenings detected", "${session.awakeningCount}", isLast = true)
                        }
                    }

                    // ── SECTION 2 — Sleep Timeline (the focus of this page) ──────
                    SectionLabel("Sleep Timeline")
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            TimelineItem(
                                time = formatTimestamp(session.sleepStart),
                                title = "Sleep Started",
                                subtitle = "Detected from a sustained inactivity gap",
                                icon = Icons.Default.Bedtime,
                                color = Color(0xFF4F46E5)
                            )

                            if (wakeEvents.isEmpty()) {
                                // Explicit, not an empty gap and not a fake event.
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 48.dp, bottom = 24.dp)
                                ) {
                                    Text(
                                        text = "No detected awakenings during this session.",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                            } else {
                                wakeEvents.forEach { event ->
                                    // Title/subtitle are assembled ONLY from the
                                    // event's own persisted fields. No inferred
                                    // cause, no "phone usage" narrative beyond the
                                    // app name and category actually recorded.
                                    TimelineItem(
                                        time = formatTimestamp(event.timestamp),
                                        title = "Awakening — ${event.appName}",
                                        subtitle = "${formatMinutes((event.duration / 60000L).toInt())} · ${event.category}",
                                        icon = Icons.Default.NotificationsActive,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                            }

                            TimelineItem(
                                time = formatTimestamp(session.sleepEnd),
                                title = "Final Wake Up",
                                subtitle = "Detected from a sustained activity cluster",
                                icon = Icons.Default.WbSunny,
                                color = Color(0xFF10B981),
                                isLast = true
                            )
                        }
                    }

                    // ── SECTION 3 — Sleep Quality (compact bar, not a hero) ──────
                    SectionLabel("Sleep Quality")
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "${session.sleepQuality}%",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = getQualityColor(session.sleepQuality)
                                )
                                Text(
                                    text = getQualityLevel(session.sleepQuality),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = getQualityColor(session.sleepQuality)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { session.sleepQuality / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = getQualityColor(session.sleepQuality),
                                trackColor = ThemeColors.background,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                // States what the number IS, without inventing a
                                // component breakdown the data model doesn't store.
                                text = "Measured by the sleep monitor for this session. The engine does not persist a per-factor breakdown of this score, so it is shown as a single measured result.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // ── SECTION 4 — Disturbance ──────────────────────────────────
                    SectionLabel("Disturbance")
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Disturbance score",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ThemeColors.textPrimary
                                )
                                Text(
                                    text = "${session.disturbanceScore}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = ThemeColors.background, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Real app activity the engine recorded inside this
                            // session's boundaries, grouped by the category it
                            // itself assigned. Descriptive only — no claim about
                            // how many points any category contributed, because
                            // that attribution is not persisted anywhere.
                            val inSessionLogs = usageLogs.filter {
                                it.startTime >= session.sleepStart && it.startTime <= session.sleepEnd
                            }
                            val categoryTotals = inSessionLogs
                                .groupBy { it.category }
                                .map { (category, logs) ->
                                    Triple(category, logs.sumOf { it.duration } / 60000L, logs.size)
                                }
                                .filter { it.second > 0L }
                                .sortedByDescending { it.second }

                            if (categoryTotals.isEmpty()) {
                                Text(
                                    text = "No app activity was recorded inside this session, so detailed factor attribution is unavailable.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    lineHeight = 16.sp
                                )
                            } else {
                                Text(
                                    text = "App activity recorded during this session",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeColors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                categoryTotals.forEach { (category, minutes, count) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(categoryColor(category), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = category,
                                                fontSize = 13.sp,
                                                color = ThemeColors.textPrimary
                                            )
                                        }
                                        Text(
                                            text = "${formatMinutes(minutes.toInt())} · $count ${if (count == 1) "session" else "sessions"}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { navController.navigate("sleep_mood_analytics") },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("View Sleep History", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = ThemeColors.textSecondary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

/** One compact label/value row. Replaces Home's large metric cards on this screen. */
@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, isLast: Boolean = false) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = label, fontSize = 13.sp, color = Color.Gray)
            }
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
        }
        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 46.dp, end = 18.dp),
                color = ThemeColors.background,
                thickness = 1.dp
            )
        }
    }
}

/** Colour per engine-assigned category. Presentation only — no data meaning. */
private fun categoryColor(category: String): Color = when (category) {
    "SOCIAL" -> Color(0xFFEF4444)
    "VIDEO" -> Color(0xFFF59E0B)
    "MESSAGING" -> Color(0xFF8B5CF6)
    "PRODUCTIVITY" -> Color(0xFF3B82F6)
    else -> Color(0xFF9CA3AF)
}

/** "2026-08-10" -> "Aug 10, 2026". Falls back to the raw string if unparseable. */
private fun sessionDateLabel(isoDate: String): String {
    val parts = isoDate.split("-")
    if (parts.size != 3) return isoDate
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val monthIndex = parts[1].toIntOrNull()?.minus(1) ?: return isoDate
    if (monthIndex !in months.indices) return isoDate
    return "${months[monthIndex]} ${parts[2].toIntOrNull() ?: parts[2]}, ${parts[0]}"
}

private fun getQualityLevel(score: Int): String = when {
    score >= 90 -> "Excellent"
    score >= 75 -> "Good"
    score >= 60 -> "Moderate"
    score >= 40 -> "Poor"
    else -> "Very Poor"
}

private fun getQualityColor(score: Int): Color = when {
    score >= 75 -> Color(0xFF10B981)
    score >= 60 -> Color(0xFFF59E0B)
    else -> Color(0xFFEF4444)
}

@Preview
@Composable
fun SleepMoodDashboardScreenPreview() {
    SleepMoodDashboardScreen(rememberNavController())
}
