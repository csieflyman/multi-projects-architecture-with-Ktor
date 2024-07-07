/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report

import fanpoll.infra.report.export.*
import fanpoll.infra.report.export.senders.GDriveReportFileSender
import fanpoll.infra.report.export.senders.ReportFileSender
import fanpoll.infra.report.export.writers.CSVReportFileWriter
import fanpoll.infra.report.export.writers.ExcelReportFileWriter
import fanpoll.infra.report.export.writers.JSONReportFileWriter
import fanpoll.infra.report.export.writers.ReportFileWriter
import fanpoll.infra.report.i18n.I18nProjectReportMessagesProvider
import io.ktor.server.application.Application
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun Application.loadReportModule() = loadKoinModules(module(createdAtStart = true) {
    single { I18nProjectReportMessagesProvider() }
    single<ReportExporter> { DefaultReportExporter() }
    single { ReportNotificationSender() }

    initReportFileWriters()

    initReportFileSenders()
})

private fun Module.initReportFileWriters() {
    single<ReportFileWriter>(named(ReportFile.MimeType.CSV)) { CSVReportFileWriter() }
    single<ReportFileWriter>(named(ReportFile.MimeType.EXCEL)) { ExcelReportFileWriter() }
    single<ReportFileWriter>(named(ReportFile.MimeType.JSON)) { JSONReportFileWriter() }
}

private fun Module.initReportFileSenders() {
    single<ReportFileSender>(named(ReportFileDestination.GDRIVE_SERVER)) { GDriveReportFileSender() }
}