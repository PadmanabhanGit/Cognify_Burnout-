package com.simats.burnouttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.simats.burnouttracker.utils.SleepWorker
import com.simats.burnouttracker.utils.RecommendationWorker
import com.simats.burnouttracker.utils.initAppContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.setProperty("compose.testing.tag.as.resource.id", "true")
        initAppContext(applicationContext)
        com.simats.burnouttracker.utils.NotificationHelper.updateWorkers(this)
        enableEdgeToEdge()
        val initialRoute = intent.getStringExtra("NAVIGATE_TO")
        setContent {
            AppNavigation(initialRoute = initialRoute)
        }
    }
}
