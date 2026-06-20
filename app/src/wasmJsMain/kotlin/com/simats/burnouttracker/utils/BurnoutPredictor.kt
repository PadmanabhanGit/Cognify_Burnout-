package com.simats.burnouttracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberBurnoutPredictor(): BurnoutPredictor {
    return remember { WebBurnoutPredictor() }
}

class WebBurnoutPredictor : BurnoutPredictor {
    override fun predict(features: BurnoutFeatures): Float {
        // Mock prediction for Web
        return 62f 
    }
}
