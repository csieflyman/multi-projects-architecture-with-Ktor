/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.data.loaders

import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.query.DynamicQuery
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.exposed.sql.dbExecute
import fanpoll.infra.database.exposed.util.toDBQuery
import fanpoll.infra.report.Report
import fanpoll.infra.report.data.table.DataTable
import org.jetbrains.exposed.sql.Database
import kotlin.reflect.KClass

class DynamicDBQueryReportDataLoader(private val objectClass: KClass<EntityDTO<*>>, private val database: Database) : ReportDataLoader {

    override suspend fun load(report: Report, queryParams: Map<String, String>?) {
        val query = queryParams?.get("query") ?: throw RequestException(
            InfraResponseCode.BAD_REQUEST_QUERYSTRING,
            "parameter query is required"
        )
        val dynamicQuery = DynamicQuery.from(query)

        val dtoList = dbExecute(database) {
            dynamicQuery.toDBQuery(objectClass).toList()
        }

        val table = DataTable(report.type.id, report.title, objectClass, dtoList, dynamicQuery.fields)
        report.dataset = listOf(table)
    }
}