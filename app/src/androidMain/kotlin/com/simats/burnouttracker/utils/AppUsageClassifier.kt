package com.simats.burnouttracker.utils

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale

class AppUsageClassifier(
    private val context: Context
) {

    private var interpreter: Interpreter

    private var vocabulary: List<String> = emptyList()

    private var vocabMap: Map<String, Int> = emptyMap()

    private var labels: List<String> = emptyList()

    init {
        try {
            interpreter = Interpreter(
                loadModelFile("ml/category_classifier.tflite")
            )
            vocabulary = loadVocabulary()
            vocabMap = vocabulary.withIndex().associate { it.value to it.index }
            labels = loadLabels()
        } catch (e: Exception) {
            e.printStackTrace()
            // Initialize with dummy if failed, or handle appropriately
            // For now, we'll just throw or let it be uninitialized if that's preferred
            throw e
        }
    }

    /**
     * MAIN CLASSIFICATION FUNCTION
     *
     * Layered so a single unreliable model isn't the only signal: a curated
     * package-name override catches apps the model is known to get wrong
     * (see [AppCategoryOverrides]), the OS's own declared app category
     * ([android.content.pm.ApplicationInfo.category], set by most Play Store
     * apps) generalizes to newly installed apps without any hardcoded list,
     * and the ML model is the last-resort fallback for whatever's left.
     */
    fun classify(packageName: String, appName: String): String {
        AppCategoryOverrides.match(packageName)?.let { return it }
        osDeclaredCategory(packageName)?.let { return it }
        return classify(appName)
    }

    /**
     * MAIN CLASSIFICATION FUNCTION (ML only, by display name)
     */
    fun classify(appName: String): String {
        return try {
            val cleaned = preprocess(appName)
            val vector = vectorize(cleaned)
            val prediction = runInference(vector)
            prediction
        } catch (e: Exception) {
            e.printStackTrace()
            "Others"
        }
    }

    /**
     * Maps Android's own app-category metadata (declared by the app in its
     * manifest, e.g. android:appCategory="game") onto this app's four
     * buckets. Returns null for CATEGORY_UNDEFINED or any category with no
     * sensible bucket (news, maps, image, ...), so those fall through to the
     * ML model instead of being force-fit into the wrong label.
     */
    private fun osDeclaredCategory(packageName: String): String? {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return null
        return try {
            val ai = context.packageManager.getApplicationInfo(packageName, 0)
            when (ai.category) {
                android.content.pm.ApplicationInfo.CATEGORY_GAME -> "Gaming"
                android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "Social Media"
                android.content.pm.ApplicationInfo.CATEGORY_AUDIO,
                android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "Streaming"
                android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * CLEAN APP NAME
     */
    private fun preprocess(text: String): String {
        return text
            .lowercase(Locale.getDefault())
            .replace(Regex("[^a-zA-Z0-9 ]"), "")
            .trim()
    }

    /**
     * TEXT -> VECTOR
     */
    private fun vectorize(text: String): FloatArray {
        val vector = FloatArray(vocabulary.size)
        val words = text.split(" ")
        for (word in words) {
            val index = vocabMap[word]
            if (index != null) {
                vector[index] += 1f
            }
        }
        return vector
    }

    /**
     * RUN TFLITE INFERENCE
     */
    private fun runInference(inputVector: FloatArray): String {
        val input = arrayOf(inputVector)
        val output = Array(1) {
            FloatArray(labels.size)
        }
        interpreter.run(input, output)
        val probabilities = output[0]

        var maxIndex = 0
        var maxConfidence = probabilities[0]

        for (i in probabilities.indices) {
            if (probabilities[i] > maxConfidence) {
                maxConfidence = probabilities[i]
                maxIndex = i
            }
        }

        /**
         * CONFIDENCE THRESHOLD
         */
        if (maxConfidence < 0.45f) {
            return "Others"
        }

        return labels[maxIndex]
    }

    /**
     * LOAD VOCAB FILE
     */
    private fun loadVocabulary(): List<String> {
        val vocabList = mutableListOf<String>()
        val reader = BufferedReader(
            InputStreamReader(
                context.assets.open("ml/vocab.txt")
            )
        )
        reader.useLines { lines ->
            lines.forEach {
                vocabList.add(it.trim())
            }
        }
        return vocabList
    }

    /**
     * LOAD LABELS FILE
     */
    private fun loadLabels(): List<String> {
        val labelList = mutableListOf<String>()
        val reader = BufferedReader(
            InputStreamReader(
                context.assets.open("ml/labels.txt")
            )
        )
        reader.useLines { lines ->
            lines.forEach {
                labelList.add(it.trim())
            }
        }
        return labelList
    }

    /**
     * LOAD TFLITE MODEL
     */
    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(
            fileDescriptor.fileDescriptor
        )
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )
    }

    /**
     * OPTIONAL TEST METHOD
     */
    fun debugTest() {
        val apps = listOf(
            "Instagram",
            "Netflix",
            "Spotify",
            "YouTube",
            "BGMI",
            "Free Fire",
            "Kindle",
            "Google Docs",
            "WhatsApp"
        )
        for (app in apps) {
            val result = classify(app)
            println("$app -> $result")
        }
    }
}
