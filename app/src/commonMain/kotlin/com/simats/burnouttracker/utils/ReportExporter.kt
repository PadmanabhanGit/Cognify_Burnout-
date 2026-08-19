package com.simats.burnouttracker.utils

import androidx.compose.runtime.Composable
import com.simats.burnouttracker.data.models.WeeklyReportData

/**
 * Turns the Weekly Report screen's real data into a PDF — built from
 * [WeeklyReportData] directly, not a screenshot of the UI, so every number in
 * the file is exactly what the screen is showing.
 */
interface ReportExporter {
    /** Builds the PDF and saves it to the device's Downloads folder. */
    fun downloadPdf(report: WeeklyReportData, burnoutRiskLevel: String, onResult: (Result<Unit>) -> Unit)

    /** Builds the PDF and opens the system share sheet for it. */
    fun shareReport(report: WeeklyReportData, burnoutRiskLevel: String, onResult: (Result<Unit>) -> Unit)
}

@Composable
expect fun rememberReportExporter(): ReportExporter
