package com.simats.burnouttracker

import com.simats.burnouttracker.ui.theme.ThemeColors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.simats.burnouttracker.data.api.ApiClient
import com.simats.burnouttracker.data.models.StartSessionRequest
import com.simats.burnouttracker.utils.AppData
import com.simats.burnouttracker.utils.PlatformSettings
import com.simats.burnouttracker.utils.rememberPlatformSettings
import com.simats.burnouttracker.utils.rememberTimerHelper
import com.simats.burnouttracker.utils.FirebaseTokenProvider
import com.simats.burnouttracker.data.StudySessionStore
import com.simats.burnouttracker.data.models.ActiveStudySession
import com.simats.burnouttracker.data.models.PendingStop
import com.simats.burnouttracker.data.models.StudyStopLifecycle
import com.simats.burnouttracker.utils.getCurrentTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.simats.burnouttracker.data.models.OfflineSessionRequest

@Composable
fun StudyTrackerScreen(navController: NavController) {
    val settings = rememberPlatformSettings("study_tracker")
    val actionPlanSettings = rememberPlatformSettings("action_plan")

    // Restore an in-flight session from disk BEFORE the first composition reads
    // AppData.activeSessionName. Previously activeSessionId / sessionStartTime were
    // in-memory only, so a process restart lost the session id and left the Firestore
    // document stranded as isActive:true forever (excluded from every total).
    // Crash / restart recovery. A persisted session is adopted only if the
    // account now signed in actually owns it — otherwise it is left untouched
    // for its owner rather than being resumed, stopped, or given an invented end
    // time. Nothing here fabricates a duration: an unresolved session stays
    // unresolved and visible.
    remember {
        if (AppData.activeSessionName == null) {
            val uid = FirebaseTokenProvider.currentUid()
            val saved = StudySessionStore.readActive(settings, uid)
            if (saved != null) {
                when (StudyStopLifecycle.recoveryFor(saved, uid, stillRunning = true)) {
                    StudyStopLifecycle.Recovery.RESUME -> {
                        AppData.activeSessionName = saved.subject
                        AppData.sessionStartTime = saved.startedAt
                        AppData.activeSessionId = saved.sessionId
                    }
                    // Not ours, or unresolvable: do not adopt and do not touch it.
                    StudyStopLifecycle.Recovery.IGNORE,
                    StudyStopLifecycle.Recovery.HAND_OFF -> Unit
                }
            }
        }
        true
    }

    var isTimerRunning by remember { mutableStateOf(AppData.activeSessionName != null) }
    var elapsedTimeSeconds by remember { mutableLongStateOf(0L) }
    var showSessionPrompt by remember { mutableStateOf(false) }
    var sessionNameInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    val timerHelper = rememberTimerHelper()

    LaunchedEffect(Unit) {
        // ── Study synchronization lifecycle ───────────────────────────────────
        // Firestore is the single source of truth. The local Settings cache is a
        // display buffer only — it is never allowed to outrank the backend.
        //
        // Order matters and is strictly sequential (this block is a suspending
        // coroutine, each call is awaited before the next begins):
        //
        //   1. paint last-known values so the screen isn't blank
        //   2. flush queued offline sessions  → Firestore becomes complete
        //   3. read /api/study/stats/weekly   → reflects step 2
        //   4. adopt those values verbatim    → stale local values corrected
        //
        // Previously step 3 ran before step 2, and step 2 was a detached
        // scope.launch with no re-read afterwards, so the fetch always saw
        // pre-flush data and required a second screen visit to reconcile.

        // 1. Provisional: last known values, replaced below.
        AppData.studyTodayHours = settings.getString("studyTodayHours", "0.0")?.toFloatOrNull() ?: 0f
        AppData.studyWeekHours = settings.getString("studyWeekHours", "0.0")?.toFloatOrNull() ?: 0f

        // 2. Flush FIRST and await completion. Promised-but-unconfirmed STOPS go
        //    before the offline queue: a stop that never landed leaves the server
        //    document isActive, and every total below excludes it until it does.
        flushPendingStops(settings)
        flushPendingStudySessions(settings)

        // 3 + 4. Read canonical Firestore-derived totals and adopt them verbatim.
        adoptCanonicalStudyStats(settings)
    }

    LaunchedEffect(isTimerRunning, AppData.sessionStartTime) {
        while (isTimerRunning) {
            val start = AppData.sessionStartTime
            if (start != null) {
                elapsedTimeSeconds = (getCurrentTimeMillis() - start) / 1000
            }
            delay(1000)
        }
    }

    if (showSessionPrompt) {
        AlertDialog(
            onDismissRequest = { showSessionPrompt = false },
            title = { Text("What are you working on?") },
            text = {
                OutlinedTextField(
                    value = sessionNameInput,
                    onValueChange = { sessionNameInput = it },
                    placeholder = { Text("e.g. Mathematics, Project Research") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = sessionNameInput.isNotBlank(),
                    onClick = {
                        val name = sessionNameInput
                        val startedAt = getCurrentTimeMillis()
                        AppData.activeSessionName = name
                        AppData.sessionStartTime = startedAt
                        isTimerRunning = true
                        showSessionPrompt = false
                        timerHelper.startTimer(name)

                        // Persist ownership so a process restart resumes this session
                        // instead of orphaning its Firestore document. The OWNER uid
                        // is recorded here, at the one moment it is unambiguous, so
                        // every later stop can verify it rather than assuming whoever
                        // is signed in at that point is the same person.
                        val ownerUid = FirebaseTokenProvider.currentUid()
                        StudySessionStore.writeActive(
                            settings,
                            ActiveStudySession(
                                sessionId = null,
                                ownerUid = ownerUid,
                                subject = name,
                                startedAt = startedAt
                            )
                        )

                        // Start session on backend
                        scope.launch {
                            try {
                                val response = ApiClient.startStudySession(StartSessionRequest(subject = name))
                                // The id is what lets this session be STOPPED later. Without it
                                // the stop call is skipped and the server keeps the session open,
                                // so a response that omits it must not be treated as a success.
                                val sessionId = response.session?.id
                                if (response.success && sessionId != null) {
                                    AppData.activeSessionId = sessionId
                                    StudySessionStore.attachSessionId(settings, sessionId)
                                }
                            } catch (e: Exception) {}
                        }

                        // Schedule local alarm if reminders are enabled
                        if (actionPlanSettings.getBoolean("study_reminders", true)) {
                            val durationStr = actionPlanSettings.getString("study_duration", "45 min") ?: "45 min"
                            val mins = durationStr.replace(" min", "").trim().toIntOrNull() ?: 45
                            com.simats.burnouttracker.utils.scheduleStudyTimer(mins)
                        }
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSessionPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val headerGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
    )

    Scaffold(
        containerColor = ThemeColors.background,
        bottomBar = { AppBottomNavigation(navController, "tracker") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section (STT Image)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(headerGradient, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(top = 40.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Study Time Tracking",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Monitor your study sessions and analytics",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset(y = (-30).dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Current Session Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Current Session", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeColors.textPrimary)
                            }
                            Surface(
                                color = if (isTimerRunning) Color(0xFFFEF08A) else Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isTimerRunning) "IN PROGRESS" else "READY",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTimerRunning) Color(0xFFA16207) else Color(0xFF16A34A)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        val displayTime = formatElapsedTime(elapsedTimeSeconds)
                        Text(
                            text = displayTime,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                if (isTimerRunning) {
                                    timerHelper.stopTimer()
                                    isTimerRunning = false
                                    com.simats.burnouttracker.utils.cancelStudyTimer()
                                    val hours = elapsedTimeSeconds / 3600f
                                    val minutes = (elapsedTimeSeconds / 60).toInt()
                                    val sessionName = AppData.activeSessionName ?: "Unknown"
                                    val stoppedSessionId = AppData.activeSessionId
                                    // Captured BEFORE anything is released, because the handoff
                                    // and the offline fallback both need it after the clear.
                                    val sessionStartedAt = AppData.sessionStartTime ?: getCurrentTimeMillis()
                                    val sessionStartIso = Instant.fromEpochMilliseconds(sessionStartedAt).toString()

                                    // ── Durable handoff, THEN release ──────────────────────
                                    // The persisted session is not cleared until its stop
                                    // exists somewhere durable. Previously these removes ran
                                    // before the POST below resolved, so a failed stop had
                                    // nothing left to retry with and the server document
                                    // stayed isActive forever, excluded from every total.
                                    //
                                    // Ownership is verified first: if the account that started
                                    // this session is not the one signed in now, no stop is
                                    // sent and nothing is cleared — the session stays for its
                                    // owner. That is the account-switch edge case.
                                    val authUid = FirebaseTokenProvider.currentUid()
                                    val persisted = StudySessionStore.readActive(settings, authUid)
                                    val ownerUid = persisted?.ownerUid ?: authUid
                                    val mayStop = StudyStopLifecycle.mayStop(ownerUid, authUid)

                                    if (mayStop) {
                                        if (stoppedSessionId != null) {
                                            // Session id continuously present in durable state:
                                            // active slot → pending stop, no gap between them.
                                            StudySessionStore.handOffStop(
                                                settings,
                                                PendingStop(
                                                    sessionId = stoppedSessionId,
                                                    ownerUid = ownerUid,
                                                    subject = sessionName,
                                                    startedAt = sessionStartedAt,
                                                    queuedAt = getCurrentTimeMillis()
                                                )
                                            )
                                        } else {
                                            // Never got a server id — nothing to stop server-side.
                                            // It is finished through the offline queue below.
                                            StudySessionStore.clearActive(settings)
                                        }
                                        AppData.activeSessionName = null
                                        AppData.activeSessionId = null
                                        AppData.sessionStartTime = null
                                        elapsedTimeSeconds = 0
                                        sessionNameInput = ""
                                    } else {
                                        println(
                                            "[STUDY] stop refused: session owner=$ownerUid " +
                                                "but authenticated=$authUid -> no request sent, session retained."
                                        )
                                    }

                                    // Update BurnoutFeatures for prediction (unchanged behaviour)
                                    AppData.currentFeatures = AppData.currentFeatures.copy(
                                        productivityHours = AppData.currentFeatures.productivityHours + hours,
                                        totalScreenTime = AppData.currentFeatures.totalScreenTime + hours
                                    )
                                    AppData.studyMonthHours += hours

                                    // Stop on backend, then reconcile against Firestore.
                                    // The local totals are NO LONGER incremented unconditionally —
                                    // that was what let Android drift above Firestore permanently.
                                    scope.launch {
                                        // Only the owner sends anything. When mayStop is false the
                                        // session was left untouched above, so there is nothing to
                                        // reconcile and nothing to queue.
                                        var syncSuccess = false
                                        if (mayStop) {
                                            syncSuccess = if (stoppedSessionId != null) {
                                                // Drives the durable record: STOPPING -> STOPPED on a
                                                // confirmed response, STOPPING -> FAILED otherwise,
                                                // with the reason persisted. Retrying is safe because
                                                // the server now returns an already-stopped session
                                                // unchanged instead of re-deriving its duration.
                                                attemptPendingStop(settings, stoppedSessionId)
                                            } else {
                                                false
                                            }
                                        }

                                        if (syncSuccess) {
                                            // Firestore now holds the authoritative duration.
                                            // Re-read it rather than trusting our local arithmetic.
                                            flushPendingStudySessions(settings)
                                            adoptCanonicalStudyStats(settings)
                                        } else if (mayStop) {
                                            // Queue offline using the existing mechanism.
                                            try {
                                                val pendingJson = settings.getString("pending_sessions", "[]") ?: "[]"
                                                val pendingQueue = try {
                                                    Json.decodeFromString<List<OfflineSessionRequest>>(pendingJson).toMutableList()
                                                } catch (e: Exception) {
                                                    mutableListOf<OfflineSessionRequest>()
                                                }
                                                pendingQueue.add(OfflineSessionRequest(subject = sessionName, duration = minutes, startTime = sessionStartIso))
                                                settings.putString("pending_sessions", Json.encodeToString(pendingQueue))
                                            } catch (e: Exception) {}

                                            // PROVISIONAL local values — shown only while the write is
                                            // unsynced so the screen isn't blank. Overwritten wholesale
                                            // by adoptCanonicalStudyStats() once the queue flushes.
                                            AppData.studyTodayHours += hours
                                            AppData.studyWeekHours += hours
                                            AppData.studyBreakdown[sessionName] = (AppData.studyBreakdown[sessionName] ?: 0f) + hours
                                            settings.putString("studyTodayHours", AppData.studyTodayHours.toString())
                                            settings.putString("studyWeekHours", AppData.studyWeekHours.toString())

                                            val localDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                            val currentDay = localDate.dayOfWeek.ordinal
                                            if (currentDay in 0..6 && currentDay < AppData.weeklyStudyData.size) {
                                                AppData.weeklyStudyData[currentDay] += hours
                                            }
                                            AppData.monthlyStudyTrend[3] = (AppData.studyWeekHours / 40f).coerceIn(0f, 1f)
                                        }
                                    }
                                } else {
                                    showSessionPrompt = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isTimerRunning) Color(0xFFEF4444) else Color(0xFF2563EB))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (isTimerRunning) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = if (isTimerRunning) "End Session" else "Start Session", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val currentAddedSeconds = if (isTimerRunning) elapsedTimeSeconds else 0L
                        val todaysDisplay = formatDisplayTime((AppData.studyTodayHours * 3600).toLong() + currentAddedSeconds)
                        val weeklyDisplay = formatDisplayTime((AppData.studyWeekHours * 3600).toLong() + currentAddedSeconds)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SessionStatItem(label = "Today's Total", value = todaysDisplay, color = Color(0xFFEFF6FF), textColor = Color(0xFF2563EB), modifier = Modifier.weight(1f))
                            SessionStatItem(label = "This Week", value = weeklyDisplay, color = Color(0xFFFAF5FF), textColor = Color(0xFF9333EA), modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 2. Weekly Overview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeColors.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Weekly Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ThemeColors.textPrimary)
                            }
                            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Bar Chart
                        Row(modifier = Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            val maxHours = AppData.weeklyStudyData.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                            AppData.weeklyStudyData.forEachIndexed { index, value ->
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                    Box(modifier = Modifier.fillMaxWidth().height(((value / maxHours) * 120).dp).background(Color(0xFF3B82F6), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = days[index], fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { navController.navigate("study_tracker_details") },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.card, contentColor = Color(0xFF9333EA)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ThemeColors.background)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "View Detailed Trends", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun SessionStatItem(label: String, value: String, color: Color, textColor: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = color) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 12.sp, color = textColor.copy(alpha = 0.7f))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }
    }
}

/**
 * Attempts one durable stop and records the outcome, returning whether the
 * server confirmed it.
 *
 * The single place a stop request is issued, so the state transitions cannot be
 * spelled differently at each call site. STOPPING is written BEFORE the request
 * so a process killed mid-call leaves a retryable record rather than a silent
 * gap; only a confirmed response removes the entry.
 *
 * Safe to call for a session already stopped on the server: /stop now returns
 * the existing session unchanged with `alreadyStopped`, so a retry after a
 * timeout confirms rather than re-deriving the duration.
 */
private suspend fun attemptPendingStop(settings: PlatformSettings, sessionId: String): Boolean {
    val now = getCurrentTimeMillis()
    StudySessionStore.readPendingStops(settings)
        .firstOrNull { it.sessionId == sessionId }
        ?.let { StudySessionStore.updateStop(settings, StudyStopLifecycle.beginStop(it, now)) }

    val outcome = try {
        val resp = ApiClient.stopStudySession(sessionId)
        if (resp.success) null else (resp.message ?: "stop rejected by server")
    } catch (e: Exception) {
        "${e::class.simpleName}: ${e.message}"
    }

    return if (outcome == null) {
        // Confirmed. STOPPED is terminal, so the record leaves the queue.
        StudySessionStore.removeStop(settings, sessionId)
        true
    } else {
        StudySessionStore.readPendingStops(settings)
            .firstOrNull { it.sessionId == sessionId }
            ?.let { StudySessionStore.updateStop(settings, StudyStopLifecycle.stopFailed(it, outcome, now)) }
        println("[STUDY] stop FAILED for $sessionId: $outcome. Retained for retry.")
        false
    }
}

/**
 * Retry every stop this account is allowed to retry.
 *
 * Ownership is re-checked per entry against the LIVE authenticated uid rather
 * than assumed from the store that was opened — so signing in as a different
 * account can never flush the previous one's queue, even in the window where
 * store resolution and Firebase auth disagree.
 */
private suspend fun flushPendingStops(settings: PlatformSettings) {
    val uid = FirebaseTokenProvider.currentUid()
    if (uid.isBlank()) return
    val retryable = StudySessionStore.retryableStops(settings, uid)
    if (retryable.isEmpty()) return

    println("[STUDY] ${retryable.size} pending stop(s) for this account; retrying.")
    for (stop in retryable) {
        // The account can change mid-flush; stop rather than send the remainder
        // under whoever signed in next.
        if (FirebaseTokenProvider.currentUid() != uid) {
            println("[STUDY] account changed mid-flush; remaining stops left pending.")
            return
        }
        attemptPendingStop(settings, stop.sessionId)
    }
}

/**
 * Push every queued offline session to Firestore via the backend.
 *
 * Suspends until the queue has been fully attempted, so callers can safely read
 * canonical stats immediately afterwards and know the read reflects the flush.
 * Entries that fail remain queued for the next attempt.
 */
private suspend fun flushPendingStudySessions(settings: PlatformSettings) {
    try {
        val pendingJson = settings.getString("pending_sessions", "[]") ?: "[]"
        val pendingQueue = try {
            Json.decodeFromString<List<OfflineSessionRequest>>(pendingJson).toMutableList()
        } catch (e: Exception) {
            mutableListOf<OfflineSessionRequest>()
        }
        if (pendingQueue.isEmpty()) return

        val syncedIndices = mutableListOf<Int>()
        for ((index, request) in pendingQueue.withIndex()) {
            val resp = ApiClient.logOfflineSession(request)
            if (resp.success) syncedIndices.add(index)
        }
        syncedIndices.sortedDescending().forEach { pendingQueue.removeAt(it) }
        settings.putString("pending_sessions", Json.encodeToString(pendingQueue))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Read /api/study/stats/weekly and adopt the Firestore-derived totals VERBATIM.
 *
 * Assignment is unconditional in both directions. There is deliberately NO
 * `backend >= local` guard: a stale local cache that sits above Firestore must be
 * corrected DOWNWARD, otherwise Android can never converge on the shared truth.
 *
 * Returns true when canonical values were adopted. On failure the previous
 * provisional values are left untouched rather than being zeroed.
 */
private suspend fun adoptCanonicalStudyStats(settings: PlatformSettings): Boolean {
    return try {
        val response = ApiClient.getStudyWeeklyStats()
        val stats = response.stats
        if (!response.success || stats == null) return false

        // totalMinutes / todayMinutes are the precise integer fields; totalHours is
        // pre-rounded to 1dp by the backend and would lose precision here.
        val backendTodayHours = (stats.todayMinutes ?: 0) / 60f
        val backendWeekHours = stats.totalMinutes / 60f

        AppData.studyTodayHours = backendTodayHours
        AppData.studyWeekHours = backendWeekHours
        settings.putString("studyTodayHours", backendTodayHours.toString())
        settings.putString("studyWeekHours", backendWeekHours.toString())

        // Mon–Sun, rebuilt from backend dailyBreakdown (already IST calendar days).
        val dayMap = stats.dailyTotals ?: stats.dailyBreakdown ?: emptyMap()
        AppData.weeklyStudyData.clear()
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
            AppData.weeklyStudyData.add((dayMap[day] ?: 0) / 60f)
        }

        // Replace, don't merge — a subject removed upstream must disappear here too.
        AppData.studyBreakdown.clear()
        stats.subjectBreakdown?.forEach { (subject, mins) ->
            AppData.studyBreakdown[subject] = mins / 60f
        }

        AppData.monthlyStudyTrend[3] = (backendWeekHours / 40f).coerceIn(0f, 1f)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun formatElapsedTime(seconds: Long): String {
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

private fun formatDisplayTime(seconds: Long): String {
    if (seconds < 3600) {
        val mins = (seconds / 60).toInt()
        return "${mins}m"
    }
    val hours = seconds / 3600f
    val formatted = (hours * 10).toInt() / 10f
    return "${formatted}H"
}
