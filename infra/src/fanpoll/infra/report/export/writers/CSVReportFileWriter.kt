/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.export.writers

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.report.Report
import fanpoll.infra.report.data.table.DataTable
import fanpoll.infra.report.export.ReportFile
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.IOException
import java.nio.charset.StandardCharsets

class CSVReportFileWriter : ReportFileWriter {
    override suspend fun write(report: Report) {
        report.file = ReportFile(report.title, ReportFile.MimeType.CSV)
        report.file.content = try {
            val printer = CSVPrinter(StringBuilder(), CSVFormat.DEFAULT)
            for (datasetItem in report.dataset) {
                val table = datasetItem as DataTable
                printer.printComment("========== " + table.name + " ==========")
                printer.printRecord(table.columns.map { it.name })
                for (row in table.data) {
                    printer.printRecord(row.map { it.stringValue() })
                }
                printer.println()
                printer.println()
            }
            printer.out.toString().toByteArray(StandardCharsets.UTF_8)
        } catch (e: IOException) {
            throw InternalServerException(InfraResponseCode.IO_ERROR, "fail to write csv report ${report.type.id}($report.id})", e)
        }
    }
}