package com.simats.burnouttracker.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class AndroidBurnoutPredictor(private val context: Context) : BurnoutPredictor {
    private var interpreter: Interpreter? = null

    init {
        try {
            val model = loadModelFile(context, "burnout_engine.tflite")
            val options = Interpreter.Options().apply {
                numThreads = 2
            }
            interpreter = Interpreter(model, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelName)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override fun predict(features: BurnoutFeatures): Float {
        if (interpreter == null) return 50f 
        
        // Convert mood string to score
        val moodScore = when (AppData.lastMoodLogged.lowercase()) {
            "happy", "great", "excellent" -> 8f
            "good" -> 7f
            "neutral", "okay" -> 5f
            "sad", "bad" -> 3f
            "terrible", "awful" -> 1f
            else -> 6f // Default average
        }
        
        // Prepare the 1x5 input array matching your model inputs: 
        // [study_hrs, sleep_hrs, mood_score, gaming_hrs, prod_hrs]
        val input = floatArrayOf(
            AppData.studyTodayHours,
            if (AppData.lastSleepLogged > 0f) AppData.lastSleepLogged else 7f,
            moodScore,
            features.gamingHours,
            features.productivityHours
        )
        val inputArray = arrayOf(input)
        
        // Prepare the 1x1 output array
        val outputArray = Array(1) { FloatArray(1) }
        
        try {
            interpreter?.run(inputArray, outputArray)
            var score = outputArray[0][0]
            
            // Constrain score between 0 and 100
            if (score < 0f) score = 0f
            if (score > 100f) score = 100f
            return score
        } catch (e: Exception) {
            e.printStackTrace()
            return 50f
        }
    }
    
    fun close() {
        interpreter?.close()
    }
}

@Composable
actual fun rememberBurnoutPredictor(): BurnoutPredictor {
    val context = LocalContext.current
    return remember { AndroidBurnoutPredictor(context) }
}
