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
