/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.report

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.report.ReportType
import fanpoll.infra.report.export.ReportFile

enum class OpsReportType(private val supportedFormats: List<ReportFile.MimeType>) : ReportType {

    UserDynamicQuery(listOf(ReportFile.MimeType.CSV, ReportFile.MimeType.EXCEL));

    override val id: String = name
    override fun isSupportedFileFormat(mimeType: ReportFile.MimeType): Boolean =
        mimeType == ReportFile.MimeType.JSON || supportedFormats.contains(mimeType)

    companion object {
        fun getById(id: String): ReportType = entries.firstOrNull { it.id == id } ?: throw RequestException(
            InfraResponseCode.BAD_REQUEST_QUERYSTRING,
            "invalid ops report type id $id"
        )
    }
}