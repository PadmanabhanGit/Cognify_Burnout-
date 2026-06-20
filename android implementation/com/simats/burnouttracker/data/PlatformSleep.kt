package com.simats.burnouttracker.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private var cachedRepository: SleepRepository? = null

fun getInternalSleepRepository(context: Context): SleepRepository {
    return cachedRepository ?: AndroidSleepRepository(context.applicationContext).also {
        cachedRepository = it
    }
}

actual fun getSleepRepository(): SleepRepository {
    // This assumes it's called from a place where we have access to context or it's been initialized
    // In Android, we often use a singleton or DI.
    // For now, let's use a workaround if we don't have a global context getter.
    // Actually, we can use the appContext from PlatformScheduler if we make it public or similar.
    return cachedRepository ?: throw IllegalStateException("Repository not initialized. Call rememberSleepRepository first or initialize global context.")
}

@Composable
actual fun rememberSleepRepository(): SleepRepository {
    val context = LocalContext.current
    return remember(context) {
        getInternalSleepRepository(context)
    }
}
