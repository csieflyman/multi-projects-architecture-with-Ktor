/*
 * Copyright (c) 2023. fanpoll All rights reserved.
 */

package fanpoll.ops.report

import fanpoll.infra.auth.authorize
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.response.respond
import fanpoll.infra.i18n.Lang
import fanpoll.infra.notification.Recipient
import fanpoll.infra.openapi.route.getWithLocation
import fanpoll.infra.openapi.route.post
import fanpoll.infra.report.Report
import fanpoll.infra.report.data.loaders.ReportDataLoader
import fanpoll.infra.report.export.*
import fanpoll.infra.report.i18n.I18nProjectReportMessagesProvider
import fanpoll.infra.session.UserSession
import fanpoll.ops.OpsAuth
import fanpoll.ops.OpsConst
import fanpoll.ops.OpsKoinContext
import fanpoll.ops.user.services.UserService
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.util.toMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import org.koin.core.qualifier.qualifier
import org.koin.ktor.ext.inject
import java.util.*

fun Route.exportReport() {

    val i18NProjectReportMessagesProvider by inject<I18nProjectReportMessagesProvider>()
    val reportExporter by inject<ReportExporter>()
    val reportNotificationSender by inject<ReportNotificationSender>()
    val userService = OpsKoinContext.koin.get<UserService>()

    authorize(OpsAuth.OpsTeam) {

        suspend fun exportReport(
            reportTypeId: String,
            reportLang: Lang? = Lang.SystemDefault,
            parameters: Map<String, String>? = null,
            fileFormat: ReportFile.MimeType,
            fileDestination: ReportFileDestination? = null
        ): Report {
            val reportType = OpsReportType.getById(reportTypeId)
            if (!reportType.isSupportedFileFormat(fileFormat))
                throw RequestException(
                    InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                    "$reportTypeId don't supported file format $fileFormat"
                )

            val report = Report(OpsConst.projectId, reportType, reportLang!!)
            val i18nReportMessages = i18NProjectReportMessagesProvider.getMessages(report.projectId, report.lang)
            report.title = i18nReportMessages.getTitle(report.type)

            val dataLoader = OpsKoinContext.koin.get<ReportDataLoader>(qualifier(reportTypeId))
            withContext(Dispatchers.IO) {
                dataLoader.load(report, parameters)
                reportExporter.export(report, ExportReportOptions(fileFormat, fileDestination))
            }
            return report
        }

        getWithLocation<GetReportJsonRequest, JsonObject>(ReportOpenApi.GetReportJson) { request ->
            val currentUser = userService.getUserById(call.sessions.get<UserSession>()!!.userId)
            val report = exportReport(
                request.reportTypeId, currentUser.lang,
                call.request.queryParameters.toMap().mapValues { it.value[0] },
                ReportFile.MimeType.JSON, null
            )
            call.respond(DataResponseDTO(report.toJson()))
        }

        getWithLocation<DownloadReportRequest, ByteArray>(ReportOpenApi.DownloadReport) { request ->
            val currentUser = userService.getUserById(call.sessions.get<UserSession>()!!.userId)
            val report = exportReport(
                request.reportTypeId, currentUser.lang,
                call.request.queryParameters.toMap().mapValues { it.value[0] },
                request.fileFormat, null
            )
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileNameAsterisk, report.file.fullName)
                    .toString()
            )
            call.respondBytes(ContentType.parse(report.file.mimeType.value), HttpStatusCode.OK) { report.file.content }
        }

        post<ExportReportRequest, UUID>(ReportOpenApi.ExportReport) { request ->
            val currentUser = userService.getUserById(call.sessions.get<UserSession>()!!.userId)
            val report = exportReport(
                request.reportTypeId, currentUser.lang, request.parameters,
                request.fileFormat, request.fileDestination
            )

            val recipients = setOf(
                with(currentUser) {
                    Recipient(id.toString(), name = name!!, email = email, lang = lang)
                }
            )
            reportNotificationSender.send(report, recipients, request.parameters)
            call.respond(DataResponseDTO(ExportReportResponse(report.id, report.file.downloadUrl)))
        }
    }
}

