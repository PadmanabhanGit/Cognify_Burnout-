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

    /**
     * The burnout model, as it is actually shipped in `app/src/main/assets`.
     *
     * This was `burnout_engine.tflite`, which does not exist anywhere in the
     * project — the file is named `burnout_predictor.tflite` and lives under
     * `ml/`. `assets.openFd` therefore threw on every construction, the catch
     * below swallowed it, and [interpreter] stayed null, so [predict] returned
     * its 50f fallback for every user on every screen. The burnout percentage
     * was never computed at all; a constant 50% was the symptom.
     */
    private val modelAsset = "ml/burnout_predictor.tflite"

    init {
        try {
            val model = loadModelFile(context, modelAsset)
            val options = Interpreter.Options().apply {
                numThreads = 2
            }
            interpreter = Interpreter(model, options).also {
                // Logged because the failure mode this replaces was SILENT: a
                // missing model produced a plausible-looking 50% rather than an
                // error, so nothing distinguished "average risk" from "the model
                // never loaded". The shapes are printed so a future input-arity
                // change is visible at startup instead of surfacing as a caught
                // exception and, again, 50%.
                println(
                    "[BURNOUT] model loaded: $modelAsset " +
                        "input=${it.getInputTensor(0).shape().joinToString("x")} " +
                        "output=${it.getOutputTensor(0).shape().joinToString("x")}"
                )
            }
        } catch (e: Exception) {
            println("[BURNOUT] MODEL FAILED TO LOAD ($modelAsset): ${e::class.simpleName}: ${e.message}")
            println("[BURNOUT] predict() will return the 50f fallback for every call until this is fixed.")
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
        // 50f here means "no model", not "average risk". Callers cannot currently
        // tell those apart — see the startup log above for which one is in play.
        if (interpreter == null) return 50f

        // The model's ACTUAL eight inputs, min-max normalised.
        //
        // The previous array was 1x5 of [study, sleep, mood, gaming, productivity]
        // against a model whose input tensor is 1x8, so every call threw
        // IllegalArgumentException and returned the 50f fallback. Study, sleep and
        // mood are not model inputs at all — they were never part of this model's
        // feature set.
        //
        // Order is taken from feature_min.npy / feature_max.npy, which ship beside
        // the model and are 8-element float64 arrays. Their ranges identify the
        // fields unambiguously at the two positions that are not hours: index 6 is
        // 20..150 (a count — appSwitchCount) and index 7 is 10..40 (minutes —
        // averageSessionMinutes). The remaining six are hour-valued and line up
        // with BurnoutFeatures' declaration order.
        val raw = floatArrayOf(
            features.socialHours,
            features.gamingHours,
            features.streamingHours,
            features.productivityHours,
            features.totalScreenTime,
            features.nightUsageHours,
            features.appSwitchCount.toFloat(),
            features.averageSessionMinutes
        )

        // Training-time bounds. The model was trained on scaled inputs, so feeding
        // raw hours would produce a confident number from an out-of-domain input —
        // the same class of silent wrongness as the missing model file.
        val featureMin = floatArrayOf(1.0f, 0.2f, 0.5f, 0.5f, 4.0f, 0.2f, 20f, 10f)
        val featureMax = floatArrayOf(5.2f, 2.1f, 2.0f, 6.0f, 9.5f, 3.0f, 150f, 40f)

        // Clamped to [0,1]: a real user can fall outside the training range, and
        // extrapolating past it is not something this model can be trusted to do.
        val input = FloatArray(raw.size) { i ->
            ((raw[i] - featureMin[i]) / (featureMax[i] - featureMin[i])).coerceIn(0f, 1f)
        }
        val inputArray = arrayOf(input)
        
        // Prepare the 1x1 output array
        val outputArray = Array(1) { FloatArray(1) }
        
        try {
            interpreter?.run(inputArray, outputArray)
            val rawOut = outputArray[0][0]

            // The model's output scale is not documented anywhere in the repo, so
            // both conventions are handled explicitly rather than assumed: a value
            // in [0,1] is a probability and is scaled to a percentage, anything
            // larger is already one. Guessing wrong in either direction would show
            // a plausible but meaningless figure — 0.62 rendered as "1%", or 62
            // rendered as "100%".
            var score = if (rawOut in 0f..1f) rawOut * 100f else rawOut

            // Constrain score between 0 and 100
            if (score < 0f) score = 0f
            if (score > 100f) score = 100f
            println("[BURNOUT] raw=$rawOut -> score=$score  norm=${input.joinToString(",") { "%.3f".format(it) }}")
            return score
        } catch (e: Exception) {
            // Also 50f, and also indistinguishable from a real result without
            // this line — an input-shape mismatch would otherwise look identical
            // to a genuine moderate score.
            println("[BURNOUT] INFERENCE FAILED: ${e::class.simpleName}: ${e.message} -> returning 50f fallback")
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
