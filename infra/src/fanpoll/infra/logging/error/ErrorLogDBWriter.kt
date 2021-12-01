/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.error

import fanpoll.infra.base.response.ResponseCodeType
import fanpoll.infra.database.custom.principalSource
import fanpoll.infra.database.sql.UUIDTable
import fanpoll.infra.database.sql.transaction
import fanpoll.infra.logging.LogMessage
import fanpoll.infra.logging.writers.LogWriter
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

class ErrorLogDBWriter : LogWriter {

    override fun write(message: LogMessage) {
        val dto = message as ErrorLog
        transaction {
            ErrorLogTable.insert {
                it[occurAt] = dto.occurAt
                it[errorCode] = dto.errorCode.value
                it[errorCodeType] = dto.errorCode.type
                it[errorMsg] = dto.errorMsg
                it[data] = dto.data
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
                it[serviceName] = dto.serviceName
                it[serviceApi] = dto.serviceApi
                it[serviceRspCode] = dto.serviceRspCode
                it[serviceReqBody] = dto.serviceReqBody
                it[serviceRspBody] = dto.serviceRspBody
            }
        }
    }
}

object ErrorLogTable : UUIDTable(name = "infra_error_log") {

    val occurAt = timestamp("occur_at")
    val errorCode = char("error_code", 4)
    val errorCodeType = enumerationByName("error_code_type", 20, ResponseCodeType::class)
    val errorMsg = text("error_msg").nullable()
    val data = text("data").nullable()

    val reqId = varchar("req_id", 32).nullable()
    val reqAt = timestamp("req_at").nullable()
    val api = varchar("api", 255).nullable()
    val headers = text("headers").nullable()
    val querystring = text("querystring").nullable()
    val reqBody = text("req_body").nullable()

    val project = varchar("project", 20).nullable()
    val function = varchar("function", 30).nullable()
    val tag = varchar("tag", 36).nullable()
    val sourceId = principalSource("source") // name "source" conflict
    val tenantId = varchar("tenant_id", 20).nullable()

    val principal = varchar("principal", 64).nullable()
    val runAs = bool("run_as")
    val clientId = varchar("client_id", 36).nullable()
    val clientVersion = varchar("client_version", 36).nullable()
    val ip = varchar("ip", 39).nullable()

    val rspAt = timestamp("rsp_at").nullable()
    val rspTime = long("rsp_time").nullable()

    val serviceName = varchar("service_name", 20).nullable()
    val serviceApi = varchar("service_api", 255).nullable()
    val serviceRspCode = varchar("service_rsp_code", 20).nullable()
    val serviceReqBody = text("service_req_body").nullable()
    val serviceRspBody = text("service_rsp_body").nullable()

    override val naturalKeys: List<Column<out Any>> = listOf(id)
    override val surrogateKey: Column<EntityID<UUID>> = id
}

