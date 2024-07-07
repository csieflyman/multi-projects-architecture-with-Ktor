/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.export

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.report.Report
import fanpoll.infra.report.export.senders.ReportFileSender
import fanpoll.infra.report.export.writers.ReportFileWriter
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.qualifier

class DefaultReportExporter : ReportExporter, KoinComponent {
    override suspend fun export(report: Report, options: ExportReportOptions) {
        if (!report.type.isSupportedFileFormat(options.fileFormat))
            throw RequestException(
                InfraResponseCode.UNSUPPORTED_OPERATION_ERROR,
                "${report.type.id} don't supported file format ${options.fileFormat}"
            )

        val koin = getKoin()
        val writer = koin.get<ReportFileWriter>(qualifier(options.fileFormat))
        writer.write(report)

        if (options.fileDestination != null) {
            val sender = koin.get<ReportFileSender>(qualifier(options.fileDestination))
            sender.send(report.file)
        }
    }
}