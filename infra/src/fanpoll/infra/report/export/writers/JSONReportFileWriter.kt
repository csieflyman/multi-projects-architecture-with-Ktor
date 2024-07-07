/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.export.writers

import fanpoll.infra.report.Report
import fanpoll.infra.report.export.ReportFile
import kotlinx.serialization.json.buildJsonObject

class JSONReportFileWriter : ReportFileWriter {
    override suspend fun write(report: Report) {
        report.file = ReportFile(report.title, ReportFile.MimeType.JSON)
        val json = buildJsonObject {
            for (datasetItem in report.dataset) {
                put(datasetItem.id, datasetItem.toJson())
            }
        }
        report.file.content = json.toString().toByteArray(Charsets.UTF_8)
    }
}