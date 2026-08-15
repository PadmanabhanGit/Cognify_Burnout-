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

        // ── Session boundary ─────────────────────────────────────────────────
        // Establishes the session before the UI is created, so no screen can
        // render while the identity is unknown or still belongs to the previous
        // account.
        //
        // alreadySignedIn = true only here: this uid comes from a session
        // Firebase persisted before this launch, which is what makes this
        // account — and no other — the rightful owner of any pre-scoping local
        // data. See AccountScope and PrefStores.adoptLegacyStores.
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.currentUser?.uid?.let { uid ->
            com.simats.burnouttracker.utils.SessionManager
                .begin(applicationContext, uid, alreadySignedIn = true)
        }

        // Safety net, not the primary path. Login and registration establish the
        // session synchronously themselves (see beginUserSession) precisely so
        // that ordering is not left to this callback; both operations here are
        // idempotent, so a duplicate notification changes nothing.
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                com.simats.burnouttracker.utils.SessionManager
                    .begin(applicationContext, uid, alreadySignedIn = false)
            } else if (com.simats.burnouttracker.utils.UserSession.uid != null) {
                com.simats.burnouttracker.utils.SessionManager.end(applicationContext)
            }
        }

        com.simats.burnouttracker.utils.NotificationHelper.updateWorkers(this)
        enableEdgeToEdge()
        val initialRoute = intent.getStringExtra("NAVIGATE_TO")
        setContent {
            AppNavigation(initialRoute = initialRoute)
        }
    }
}
