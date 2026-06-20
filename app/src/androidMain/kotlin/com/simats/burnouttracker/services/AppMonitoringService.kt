package com.simats.burnouttracker.services

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.util.Calendar

/**
 * Service to monitor foreground app usage during night hours (10 PM - 6 AM)
 * Captures which apps user is using while sleeping
 */
class AppMonitoringService : Service() {

    private val TAG = "AppMonitoringService"
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service Started")
        if (!isRunning) {
            isRunning = true
            startMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoring() {
        // Check every 30 minutes during night hours
        handler.postDelayed({
            if (isNightTime()) {
                captureCurrentApp()
            }
            startMonitoring()
        }, 30 * 60 * 1000) // 30 minutes
    }

    /**
     * Check if current time is between 10 PM and 6 AM
     */
    private fun isNightTime(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour < 6 // 10 PM (22:00) to 6 AM (06:00)
    }

    /**
     * Capture the foreground app currently running
     */
    private fun captureCurrentApp() {
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            val endTime = System.currentTimeMillis()
            val startTime = endTime - (1000 * 60) // Last 1 minute

            val usageList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ).sortedByDescending { it.lastTimeUsed }

            if (usageList.isNotEmpty()) {
                val foregroundApp = usageList[0]
                Log.d(TAG, "Foreground App: ${foregroundApp.packageName} at ${java.util.Date()}")

                // Save to shared preferences or database
                saveAppUsageLog(foregroundApp.packageName, foregroundApp.totalTimeInForeground)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing foreground app", e)
        }
    }

    /**
     * Save app usage log to local storage
     */
    private fun saveAppUsageLog(packageName: String, duration: Long) {
        val sharedPref = getSharedPreferences("app_monitoring", Context.MODE_PRIVATE)
        val existingJson = sharedPref.getString("night_apps", "[]") ?: "[]"

        try {
            val jsonArray = if (existingJson.isBlank() || existingJson == "[]") {
                org.json.JSONArray()
            } else {
                org.json.JSONArray(existingJson)
            }
            val newEntry = org.json.JSONObject().apply {
                put("package", packageName)
                put("time", System.currentTimeMillis())
                put("duration", duration)
            }
            jsonArray.put(newEntry)
            sharedPref.edit().putString("night_apps", jsonArray.toString()).apply()
            Log.d(TAG, "App usage logged: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving app usage log", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "Service Destroyed")
    }
}

