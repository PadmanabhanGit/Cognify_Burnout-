package com.simats.burnouttracker.data

import android.content.Context
import com.simats.burnouttracker.data.database.*
import com.simats.burnouttracker.data.models.*
import com.simats.burnouttracker.utils.SleepMonitoringEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

class AndroidSleepRepository(private val context: Context) : SleepRepository {
    private val database = SleepDatabase.getDatabase(context)
    private val dao = database.sleepDao()
    private val engine = SleepMonitoringEngine(context)

    override fun getRecentSessions(): Flow<List<SleepSessionData>> {
        return dao.getRecentSessions().map { sessions ->
            sessions.map { it.toData() }
        }
    }

    override suspend fun getSessionDetails(sessionId: Long): SleepSessionData? {
        // Find session by ID - need to add this to DAO
        // For now, return null or implement
        return null 
    }

    override suspend fun getWakeEvents(sessionId: Long): List<WakeEventData> {
        return dao.getWakeEventsForSession(sessionId).map { it.toData() }
    }

    override suspend fun getUsageLogs(sessionId: Long): List<AppUsageLogData> {
        return dao.getUsageLogsForSession(sessionId).map { it.toData() }
    }

    override suspend fun refreshSleepData() {
        // Refresh last 3 days to be sure
        val cal = Calendar.getInstance()
        for (i in 0..2) {
            engine.analyzeNight(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
    }

    private fun SleepSession.toData() = SleepSessionData(
        id, date, sleepStart, sleepEnd, totalSleepMinutes, awakeningCount, sleepQuality, disturbanceScore
    )

    private fun WakeEvent.toData() = WakeEventData(
        id, sessionId, timestamp, duration, appName, packageName, category
    )

    private fun AppUsageLog.toData() = AppUsageLogData(
        id, sessionId, startTime, endTime, duration, appName, packageName, category
    )
}
