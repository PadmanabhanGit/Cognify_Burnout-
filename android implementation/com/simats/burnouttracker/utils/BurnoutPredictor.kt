package com.simats.burnouttracker.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

@Composable
actual fun rememberBurnoutPredictor(): BurnoutPredictor {
    val context = LocalContext.current
    return remember { AndroidBurnoutPredictor(context) }
}

class AndroidBurnoutPredictor(private val context: Context) : BurnoutPredictor {
    private var interpreter: Interpreter? = null

    init {
        try {
            interpreter = Interpreter(loadModelFile("ml/burnout_predictor.tflite"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    override fun predict(features: BurnoutFeatures): Float {
        if (interpreter == null) return 62f

        val input = arrayOf(
            floatArrayOf(
                normalize(features.socialHours, 0f, 12f),
                normalize(features.gamingHours, 0f, 10f),
                normalize(features.streamingHours, 0f, 10f),
                normalize(features.productivityHours, 0f, 12f),
                normalize(features.totalScreenTime, 0f, 18f),
                normalize(features.nightUsageHours, 0f, 8f),
                normalize(features.appSwitchCount.toFloat(), 0f, 300f),
                normalize(features.averageSessionMinutes, 0f, 120f)
            )
        )

        val output = Array(1) { FloatArray(1) }

        return try {
            interpreter?.run(input, output)
            output[0][0] * 100f
        } catch (e: Exception) {
            e.printStackTrace()
            62f
        }
    }

    private fun normalize(value: Float, min: Float, max: Float): Float {
        if (max == min) return 0f
        return ((value - min) / (max - min)).coerceIn(0f, 1f)
    }
}
