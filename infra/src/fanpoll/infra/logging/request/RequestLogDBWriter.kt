/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.request

import fanpoll.infra.database.exposed.sql.dbExecute
import fanpoll.infra.database.exposed.sql.principalSourceColumn
import fanpoll.infra.database.exposed.sql.userTypeColumn
import fanpoll.infra.logging.LogEntity
import fanpoll.infra.logging.writers.LogWriter
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.duration
import org.jetbrains.exposed.sql.javatime.timestamp

class RequestLogDBWriter(private val db: Database) : LogWriter {

    override suspend fun write(logEntity: LogEntity) {
        val requestLog = logEntity as RequestLog
        dbExecute(db) {
            RequestLogTable.insert {
                it[id] = requestLog.id

                it[project] = requestLog.project
                it[function] = requestLog.function
                it[sourceId] = requestLog.source
                it[principalId] = requestLog.principalId
                it[tags] = requestLog.tags?.toString()

                requestLog.user?.let { user ->
                    it[userType] = user.type
                    it[userId] = user.id
                    it[runAs] = user.runAs
                }

                requestLog.request.let { req ->
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

                requestLog.response.let { rsp ->
                    it[rspAt] = rsp.at
                    it[rspStatus] = rsp.status
                    it[rspBody] = rsp.body
                    it[duration] = rsp.duration
                }
            }
        }
    }
}

object RequestLogTable : UUIDTable(name = "infra_request_log") {

    val project = varchar("project", 20)
    val function = varchar("function", 30)
    val sourceId = principalSourceColumn("source") // name "source" conflict
    val principalId = varchar("principal_id", 64).nullable()
    val tags = text("tags").nullable()

    val userType = userTypeColumn("user_type").nullable()
    val userId = uuid("user_id").nullable()
    val runAs = bool("run_as").nullable()

    val traceId = char("trace_id", 32).nullable()
    val reqId = varchar("req_id", 36)
    val reqAt = timestamp("req_at")
    val api = varchar("api", 255)
    val headers = text("headers").nullable()
    val querystring = text("querystring").nullable()
    val reqBody = text("req_body").nullable()
    val ip = varchar("ip", 39).nullable()
    val clientId = varchar("client_id", 36).nullable()
    val clientVersion = varchar("client_version", 36).nullable()

    val rspAt = timestamp("rsp_at")
    val rspStatus = integer("rsp_status")
    val rspBody = text("rsp_body").nullable()
    val duration = duration("duration")
}