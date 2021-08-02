/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.report.util

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.extension.LocalDateRange
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.request.MyCallLoggingFeature.Feature.ATTRIBUTE_KEY_TAG
import fanpoll.infra.report.Report
import io.ktor.application.ApplicationCall
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import java.time.ZoneId

interface ReportLocation {

    val reportIds: String
    val zone: String
    val startTime: String
    val endTime: String
    val compareStartTime: String?
    val compareEndTime: String?
    val compareTimeUnit: CompareTimeUnit?

    private fun validateQueryParameters() {
        if (reportIds.isBlank())
            throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "reportIds can't be blank")

        if ((compareStartTime != null).xor(compareEndTime != null))
            throw RequestException(
                InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                "compareStartTime and compareEndTime are mutually necessary"
            )

        if (compareStartTime != null && compareTimeUnit != null)
            throw RequestException(
                InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                "compareStartTime, compareEndTime and compareTo are mutually exclusively"
            )
    }

    fun toReportQueryParameters(allReports: List<Report>, call: ApplicationCall): ReportQueryParameters {
        validateQueryParameters()

        val reports = reportIds.split(",").map { reportId ->
            allReports.find { it.id == reportId }
                ?: throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "invalid reportId: $reportId")
        }
        call.attributes.put(ATTRIBUTE_KEY_TAG, reportIds)

        val zoneId = try {
            ZoneId.of(zone)
        } catch (e: Exception) {
            throw RequestException(InfraResponseCode.BAD_REQUEST_QUERYSTRING, "invalid zone: $zone")
        }

        val range = LocalDateRange.parse(startTime, endTime)

        val compareRange: LocalDateRange? = when {
            compareTimeUnit != null -> ReportUtils.computePreviousRange(range, compareTimeUnit!!)
            compareStartTime != null && compareEndTime != null -> LocalDateRange.parse(
                compareStartTime!!,
                compareEndTime!!
            )
            else -> null
        }

        val reportParameters: MutableMap<Report, MutableMap<String, String>> = hashMapOf()
        call.request.queryParameters.forEach { key, values ->
            val report = reports.find { report -> key.startsWith("${report.id}.") }
            if (report != null) {
                val valueMap = reportParameters.getOrPut(report, { hashMapOf() })
                valueMap[key.substringAfter(".")] = values[0]
            }
        }

        return ReportQueryParameters(reports, zoneId, range, compareRange, reportParameters)
    }
}

@OptIn(KtorExperimentalLocationsAPI::class)
@Location("/reports")
data class DefaultReportLocation(
    override val reportIds: String,
    override val zone: String,
    override val startTime: String, override val endTime: String,
    override val compareStartTime: String? = null, override val compareEndTime: String? = null,
    override val compareTimeUnit: CompareTimeUnit? = null
) : ReportLocation