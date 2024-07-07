/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.export

import fanpoll.infra.base.form.Form
import fanpoll.infra.base.json.kotlinx.UUIDSerializer
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import io.ktor.server.locations.Location
import kotlinx.serialization.Serializable
import java.util.*

@OptIn(KtorExperimentalLocationsAPI::class)
@Location("")
class GetReportJsonRequest(
    val reportTypeId: String,
    val parameters: Map<String, String>? = null
) : fanpoll.infra.base.location.Location()

@OptIn(KtorExperimentalLocationsAPI::class)
@Location("/download")
class DownloadReportRequest(
    val reportTypeId: String,
    val parameters: Map<String, String>? = null,
    val fileFormat: ReportFile.MimeType
) : fanpoll.infra.base.location.Location()

@Serializable
class ExportReportRequest(
    val reportTypeId: String,
    val parameters: Map<String, String>? = null,
    val fileFormat: ReportFile.MimeType,
    val fileDestination: ReportFileDestination? = null
) : Form<ExportReportRequest>()

@Serializable
class ExportReportResponse(
    @Serializable(with = UUIDSerializer::class) val reportId: UUID,
    val downloadUrl: String?
)

