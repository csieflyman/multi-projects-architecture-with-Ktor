/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.error

import fanpoll.infra.base.exception.ExceptionUtils
import fanpoll.infra.base.response.ResponseCodeType
import fanpoll.infra.database.custom.principalSource
import fanpoll.infra.database.custom.userType
import fanpoll.infra.database.sql.UUIDTable
import fanpoll.infra.database.sql.transaction
import fanpoll.infra.logging.LogEntity
import fanpoll.infra.logging.writers.LogWriter
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.duration
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

class ErrorLogDBWriter : LogWriter {

    override fun write(logEntity: LogEntity) {
        val errorLog = logEntity as ErrorLog
        transaction {
            ErrorLogTable.insert {
                it[id] = errorLog.id

                it[occurAt] = errorLog.occurAt
                it[errorCode] = errorLog.exception.code.value
                it[errorCodeType] = errorLog.exception.code.type
                it[errorMsg] = errorLog.exception.message
                it[stackTrace] = ExceptionUtils.getStackTrace(errorLog.exception)
                it[extras] = errorLog.exception.dataMap?.toString()

                it[project] = errorLog.project
                it[function] = errorLog.function
                it[sourceId] = errorLog.source
                it[tenantId] = errorLog.tenantId?.value
                it[principalId] = errorLog.principalId
                it[tags] = errorLog.tags?.toString()

                errorLog.user?.let { user ->
                    it[userType] = user.type
                    it[userId] = user.id
                    it[runAs] = user.runAs
                }

                errorLog.request?.let { req ->
                    it[traceId] = req.traceId
                    it[reqId] = req.id
                    it[reqAt] = req.at
                    it[api] = "${req.method} ${req.path}"
                    it[headers] = req.headers?.toString()
                    it[querystring] = req.querystring
                    it[reqBody] = req.body
                    it[ip] = req.ip
                    it[clientId] = req.clientId
                    it[clientVersion] = req.clientVersion
                }

                errorLog.response?.let { rsp ->
                    it[rspAt] = rsp.at
                    it[rspStatus] = rsp.status
                    it[rspBody] = rsp.body
                    it[duration] = rsp.duration
                }

                errorLog.serviceRequest?.let { serviceReq ->
                    it[serviceName] = serviceReq.name
                    it[serviceApi] = serviceReq.api
                    it[serviceReqId] = serviceReq.reqId
                    it[serviceReqAt] = serviceReq.reqAt
                    it[serviceReqBody] = serviceReq.reqBody
                    it[serviceRspCode] = serviceReq.rspCode
                    it[serviceRspAt] = serviceReq.rspAt
                    it[serviceRspBody] = serviceReq.rspBody
                    it[serviceDuration] = serviceReq.duration
                }
            }
        }
    }
}

object ErrorLogTable : UUIDTable(name = "infra_error_log") {

    val occurAt = timestamp("occur_at")
    val errorCode = char("error_code", 4)
    val errorCodeType = enumerationByName("error_code_type", 20, ResponseCodeType::class)
    val errorMsg = text("error_msg").nullable()
    val stackTrace = text("stacktrace").nullable()
    val extras = text("extras").nullable()

    val project = varchar("project", 20)
    val function = varchar("function", 30)
    val sourceId = principalSource("source") // name "source" conflict
    val tenantId = varchar("tenant_id", 20).nullable()
    val principalId = varchar("principal_id", 64).nullable()
    val tags = text("tags").nullable()

    val userType = userType("user_type").nullable()
    val userId = uuid("user_id").nullable()
    val runAs = bool("run_as").nullable()

    val traceId = char("trace_id", 32).nullable()
    val reqId = varchar("req_id", 36).nullable()
    val reqAt = timestamp("req_at").nullable()
    val api = varchar("api", 255).nullable()
    val headers = text("headers").nullable()
    val querystring = text("querystring").nullable()
    val reqBody = text("req_body").nullable()
    val ip = varchar("ip", 39).nullable()
    val clientId = varchar("client_id", 36).nullable()
    val clientVersion = varchar("client_version", 36).nullable()

    val rspAt = timestamp("rsp_at").nullable()
    val rspStatus = integer("rsp_status").nullable()
    val rspBody = text("rsp_body").nullable()
    val duration = duration("duration").nullable()

    val serviceName = varchar("service_name", 20).nullable()
    val serviceApi = varchar("service_api", 255).nullable()
    val serviceReqId = varchar("service_req_id", 255).nullable()
    val serviceReqAt = timestamp("service_req_at").nullable()
    val serviceReqBody = text("service_req_body").nullable()
    val serviceRspCode = varchar("service_rsp_code", 20).nullable()
    val serviceRspAt = timestamp("service_rsp_at").nullable()
    val serviceRspBody = text("service_rsp_body").nullable()
    val serviceDuration = duration("service_duration").nullable()

    override val naturalKeys: List<Column<out Any>> = listOf(id)
    override val surrogateKey: Column<EntityID<UUID>> = id
}

