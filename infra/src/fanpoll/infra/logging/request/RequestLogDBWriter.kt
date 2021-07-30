/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.request

import fanpoll.infra.database.custom.principalSource
import fanpoll.infra.database.sql.UUIDTable
import fanpoll.infra.database.sql.transaction
import fanpoll.infra.logging.LogMessage
import fanpoll.infra.logging.writers.LogWriter
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.insert
import java.util.*

class RequestLogDBWriter : LogWriter {

    override fun write(message: LogMessage) {
        val dto = message as RequestLog
        transaction {
            RequestLogTable.insert {
                it[reqId] = dto.reqId
                it[reqAt] = dto.reqAt
                it[api] = dto.api
                it[headers] = dto.headers
                it[querystring] = dto.querystring
                it[reqBody] = dto.reqBody
                it[project] = dto.project
                it[function] = dto.function
                it[tag] = dto.tag
                it[sourceId] = dto.source
                it[tenantId] = dto.tenantId?.value
                it[principal] = dto.principal
                it[runAs] = dto.runAs
                it[clientId] = dto.clientId
                it[clientVersion] = dto.clientVersion
                it[ip] = dto.ip
                it[rspAt] = dto.rspAt
                it[rspTime] = dto.rspTime
                it[rspStatus] = dto.rspStatus
                it[rspBody] = dto.rspBody
            }
        }
    }
}

object RequestLogTable : UUIDTable(name = "infra_request_log") {

    val reqId = varchar("req_id", 32)
    val reqAt = timestamp("req_at")
    val api = varchar("api", 255)
    val headers = text("headers").nullable()
    val querystring = text("querystring").nullable()
    val reqBody = text("req_body").nullable()

    val project = varchar("project", 20)
    val function = varchar("function", 30)
    val tag = varchar("tag", 36).nullable()
    val sourceId = principalSource("source") // name "source" conflict
    val tenantId = varchar("tenant_id", 20).nullable()

    val principal = varchar("principal", 64).nullable()
    val runAs = bool("run_as")
    val clientId = varchar("client_id", 36).nullable()
    val clientVersion = varchar("client_version", 10).nullable()
    val ip = varchar("ip", 39).nullable()

    val rspAt = timestamp("rsp_at")
    val rspTime = long("rsp_time")
    val rspStatus = integer("rsp_status")
    val rspBody = text("rsp_body").nullable()

    override val naturalKeys: List<Column<out Any>> = listOf(id)
    override val surrogateKey: Column<EntityID<UUID>> = id
}