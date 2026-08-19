package com.simats.burnouttracker.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.simats.burnouttracker.data.models.WeeklyReportData
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class AndroidReportExporter(private val context: Context) : ReportExporter {

    override fun downloadPdf(report: WeeklyReportData, burnoutRiskLevel: String, onResult: (Result<Unit>) -> Unit) {
        try {
            val fileName = fileNameFor(report)
            val bytes = renderPdfBytes(report, burnoutRiskLevel)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Scoped storage: no permission needed, lands in the real
                // Downloads folder, visible in the Files app immediately.
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("MediaStore did not return a Uri for the new download")
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: error("Could not open an output stream for the download")
            } else {
                // Pre-scoped-storage devices (API < 29, within this app's
                // minSdk 24): WRITE_EXTERNAL_STORAGE is a runtime permission
                // here, and requesting it is a bigger scope than this fix —
                // share the generated file instead so the user can still get
                // it, rather than silently failing.
                shareReport(report, burnoutRiskLevel, onResult)
                return
            }

            Toast.makeText(context, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
            onResult(Result.success(Unit))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Couldn't save the PDF: ${e.message}", Toast.LENGTH_LONG).show()
            onResult(Result.failure(e))
        }
    }

    override fun shareReport(report: WeeklyReportData, burnoutRiskLevel: String, onResult: (Result<Unit>) -> Unit) {
        try {
            val fileName = fileNameFor(report)
            val bytes = renderPdfBytes(report, burnoutRiskLevel)

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(reportsDir, fileName)
            FileOutputStream(file).use { it.write(bytes) }

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Cognify Weekly Report — ${report.period.from} to ${report.period.to}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Share Weekly Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            onResult(Result.success(Unit))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Couldn't share the PDF: ${e.message}", Toast.LENGTH_LONG).show()
            onResult(Result.failure(e))
        }
    }

    private fun fileNameFor(report: WeeklyReportData): String =
        "Cognify_WeeklyReport_${report.period.from}_to_${report.period.to}.pdf"

    /**
     * Draws the report as text/lists directly onto a PDF canvas — every value
     * read straight from [report], the exact object the screen renders, so
     * nothing here can drift from what's on screen. Deliberately not a
     * screenshot of the Compose UI: capturing arbitrary Composable content to
     * a bitmap needs a graphicsLayer snapshot with its own failure modes,
     * and buys nothing over drawing the real numbers directly.
     */
    private fun renderPdfBytes(report: WeeklyReportData, burnoutRiskLevel: String): ByteArray {
        val pageWidth = 595 // A4 at 72dpi
        val pageHeight = 842
        val margin = 40f

        val ink = Color.parseColor("#1D2233")
        val accent = Color.parseColor("#4A3F8C")
        val body = Color.parseColor("#2A2E3D")
        val muted = Color.parseColor("#6B7280")

        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true; color = ink; isAntiAlias = true }
        val sectionPaint = Paint().apply { textSize = 14f; isFakeBoldText = true; color = accent; isAntiAlias = true }
        val bodyPaint = Paint().apply { textSize = 11f; color = body; isAntiAlias = true }
        val mutedPaint = Paint().apply { textSize = 10f; color = muted; isAntiAlias = true }

        val pdf = PdfDocument()
        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin

        fun newPageIfNeeded() {
            if (y + 16f > pageHeight - margin) {
                pdf.finishPage(page)
                pageNumber++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = margin
            }
        }

        fun line(text: String, paint: Paint, gapAfter: Float = 16f) {
            newPageIfNeeded()
            canvas.drawText(text, margin, y, paint)
            y += gapAfter
        }

        fun bulletList(items: List<String>) {
            if (items.isEmpty()) {
                line("None this week.", mutedPaint)
            } else {
                items.forEach { line("• $it", bodyPaint) }
            }
        }

        line("Cognify — Weekly Report", titlePaint, 26f)
        line("${report.period.from} to ${report.period.to}", mutedPaint, 24f)

        line("Executive Summary", sectionPaint, 20f)
        line("Total Study Time: ${report.summary.totalStudyHours} h", bodyPaint)
        line("Average Sleep: ${report.summary.avgSleep} h", bodyPaint)
        line("Average Mood: ${report.summary.avgMood} / 10", bodyPaint)
        line("Productivity: ${report.summary.avgProductivity.toInt()}%", bodyPaint)
        line("Burnout Risk Level: $burnoutRiskLevel", bodyPaint, 24f)

        line("Daily Activity", sectionPaint, 20f)
        if (report.dailyActivity.isEmpty()) {
            line("No data recorded this week.", mutedPaint, 24f)
        } else {
            report.dailyActivity.forEach { day ->
                val study = day.studyMinutes?.let { "$it min" } ?: "—"
                val sleep = day.sleepHours?.let { "$it h" } ?: "—"
                line("${day.date}   Study: $study   Sleep: $sleep", bodyPaint)
            }
            y += 8f
        }

        line("Mood & Productivity", sectionPaint, 20f)
        if (report.moodVsProductivity.isEmpty()) {
            line("No data recorded this week.", mutedPaint, 24f)
        } else {
            report.moodVsProductivity.forEach { day ->
                val mood = day.moodScore?.let { "$it/10" } ?: "—"
                val prod = day.productivityScore?.let { "$it%" } ?: "—"
                line("${day.date}   Mood: $mood   Productivity: $prod", bodyPaint)
            }
            y += 8f
        }

        line("Wellness Radar", sectionPaint, 20f)
        val wr = report.wellnessRadar
        line("Sleep: ${wr.sleep}   Mood: ${wr.mood}   Study: ${wr.study}   Productivity: ${wr.productivity}   Balance: ${wr.balance}", bodyPaint, 24f)

        line("Achievements", sectionPaint, 20f)
        bulletList(report.achievements)
        y += 8f

        line("Areas of Concern", sectionPaint, 20f)
        bulletList(report.concerns)
        y += 8f

        line("Recommendations", sectionPaint, 20f)
        bulletList(report.recommendations)

        pdf.finishPage(page)

        val out = ByteArrayOutputStream()
        pdf.writeTo(out)
        pdf.close()
        return out.toByteArray()
    }
}

@Composable
actual fun rememberReportExporter(): ReportExporter {
    val context = LocalContext.current
    return remember { AndroidReportExporter(context) }
}
