/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.report

import fanpoll.infra.DataResponseDTO
import fanpoll.infra.database.myTransaction
import fanpoll.infra.report.utils.ReportQueryParameters
import fanpoll.infra.utils.DateTimeUtils
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import mu.KotlinLogging

object ReportService {

    private val logger = KotlinLogging.logger {}

    fun queryToJson(parameters: ReportQueryParameters): DataResponseDTO {
        logger.debug { "parameters = $parameters" }

        parameters.reports.forEach { report ->
            report.reporter.validate(parameters, parameters.getReportParameters(report))
        }

        val resultMap = myTransaction {
            if (parameters.reports.count() == 1 && parameters.reports.first().reporter is CompositeReporter) {
                // flatten
                val report = parameters.reports.first()
                val result =
                    report.reporter.queryToJson(parameters, parameters.getReportParameters(report)).jsonObject
                logger.debug { result }
                result
            } else {
                parameters.reports.map { report ->
                    val json = report.reporter.queryToJson(parameters, parameters.getReportParameters(report))
                    val result = report.id to json
                    logger.debug { result }
                    result
                }.toMap()
            }
        }

        val dataMap = if (parameters.compareRange != null) {
            val map = hashMapOf<String, JsonElement>()
            map.putAll(resultMap)
            map["compareStartTime"] =
                JsonPrimitive(DateTimeUtils.LOCAL_DATE_FORMATTER.format(parameters.compareRange.start))
            map["compareEndTime"] =
                JsonPrimitive(DateTimeUtils.LOCAL_DATE_FORMATTER.format(parameters.compareRange.endInclusive))
            map
        } else resultMap
        return DataResponseDTO(JsonObject(dataMap))
    }
}