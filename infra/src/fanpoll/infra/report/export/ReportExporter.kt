/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.export

import fanpoll.infra.report.Report

interface ReportExporter {
    suspend fun export(report: Report, options: ExportReportOptions)
}

class ExportReportOptions(
    val fileFormat: ReportFile.MimeType,
    val fileDestination: ReportFileDestination? = null
)