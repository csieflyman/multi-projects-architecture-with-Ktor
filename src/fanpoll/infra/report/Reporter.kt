/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.report

import fanpoll.infra.base.extension.LocalDateRange
import fanpoll.infra.base.json.json
import fanpoll.infra.report.util.ReportQueryParameters
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

interface Reporter {

    fun validate(reportQueryParameters: ReportQueryParameters, reportParameters: MutableMap<String, String>?) {}

    fun queryToJson(reportQueryParameters: ReportQueryParameters, reportParameters: MutableMap<String, String>?): JsonElement

}

interface StatsReporter<T : StatsResult<T>> : Reporter {

    fun query(
        reportQueryParameters: ReportQueryParameters,
        range: LocalDateRange,
        reportParameters: MutableMap<String, String>?
    ): T

    @OptIn(InternalSerializationApi::class)
    override fun queryToJson(
        reportQueryParameters: ReportQueryParameters,
        reportParameters: MutableMap<String, String>?
    ): JsonElement {
        val queryResult = query(reportQueryParameters, reportParameters)
        val result = queryResult.first
        val compareResult = queryResult.second
        val serializer = result.javaClass.kotlin.serializer()
        val resultMap = hashMapOf<String, JsonElement>()
        resultMap["result"] = json.encodeToJsonElement(serializer, queryResult.first)
        if (compareResult != null)
            resultMap["compareResult"] = json.encodeToJsonElement(serializer, compareResult)
        return JsonObject(resultMap)
    }

    private fun query(reportQueryParameters: ReportQueryParameters, reportParameters: MutableMap<String, String>?): Pair<T, T?> {
        val result = query(reportQueryParameters, reportQueryParameters.range, reportParameters)
        val compareResult = if (reportQueryParameters.compareRange != null) {
            val cResult = query(reportQueryParameters, reportQueryParameters.compareRange, reportParameters)
            cResult.compare(result)
            cResult
        } else null
        return result to compareResult
    }
}

interface StatsResult<in T> {

    fun compare(newResult: T) {}
}

interface TableReporter<T : Any> : Reporter {

    val resultKClass: KClass<T>

    fun query(
        reportQueryParameters: ReportQueryParameters,
        reportParameters: MutableMap<String, String>?
    ): List<T>

    @OptIn(InternalSerializationApi::class)
    override fun queryToJson(
        reportQueryParameters: ReportQueryParameters,
        reportParameters: MutableMap<String, String>?
    ): JsonElement {
        val queryResult = query(reportQueryParameters, reportParameters)
        return json.encodeToJsonElement(ListSerializer(resultKClass.serializer()), queryResult)
    }
}

class CompositeReporter(private vararg val reports: Report) : Reporter {

    override fun validate(reportQueryParameters: ReportQueryParameters, reportParameters: MutableMap<String, String>?) {
        reports.forEach { it.reporter.validate(reportQueryParameters, reportParameters) }
    }

    override fun queryToJson(
        reportQueryParameters: ReportQueryParameters,
        reportParameters: MutableMap<String, String>?
    ): JsonElement {
        return JsonObject(reports.associate {
            it.id to it.reporter.queryToJson(reportQueryParameters, reportParameters)
        })
    }
}