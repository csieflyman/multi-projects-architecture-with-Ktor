/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.report

import fanpoll.infra.openapi.route.RouteApiOperation
import fanpoll.infra.openapi.schema.Tag

object ReportOpenApi {

    private val ReportTag = Tag("report")

    val GetReportJson = RouteApiOperation("GetReportJson", listOf(ReportTag))
    val DownloadReport = RouteApiOperation("DownloadReport", listOf(ReportTag))
    val ExportReport = RouteApiOperation("ExportReport", listOf(ReportTag))
}