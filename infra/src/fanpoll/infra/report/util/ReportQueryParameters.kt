/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.report.util

import fanpoll.infra.base.extension.LocalDateRange
import fanpoll.infra.report.Report
import java.time.ZoneId

data class ReportQueryParameters(
    val reports: List<Report>,
    val zoneId: ZoneId,
    val range: LocalDateRange,
    val compareRange: LocalDateRange?,
    private val reportParameters: MutableMap<Report, MutableMap<String, String>>
) {

    fun getReportParameters(report: Report): MutableMap<String, String>? {
        return reportParameters[report]
    }

    fun setReportParameterValue(report: Report, key: String, value: String) {
        val valueMap = reportParameters.getOrPut(report) { hashMapOf() }
        valueMap[key] = value
    }
}