/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login.logging

import fanpoll.infra.auth.login.LoginResultCode
import fanpoll.infra.database.exposed.sql.dbExecute
import fanpoll.infra.logging.LogEntity
import fanpoll.infra.logging.writers.LogWriter
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp

class LoginLogDBWriter : LogWriter {

    override suspend fun write(logEntity: LogEntity) {
        val loginLog = logEntity as LoginLog
        dbExecute {
            LoginLogTable.insert {
                it[userId] = loginLog.userId
                it[resultCode] = loginLog.resultCode
                it[occurAt] = loginLog.occurAt
                it[project] = loginLog.project
                it[sourceName] = loginLog.sourceName
                it[clientId] = loginLog.clientId
                it[clientVersion] = loginLog.clientVersion
                it[ip] = loginLog.ip
                it[traceId] = loginLog.traceId
                it[sid] = loginLog.sid
            }
        }
    }
}

object LoginLogTable : UUIDTable(name = "infra_login_log") {

    val traceId = char("trace_id", 32).nullable()
    val userId = uuid("user_id")
    val resultCode = enumerationByName("result_code", 20, LoginResultCode::class)
    val occurAt = timestamp("occur_at")

    val project = varchar("project", 20)
    val sourceName = varchar("source", 30) // name "source" conflict
    val clientId = varchar("client_id", 36).nullable()
    val clientVersion = varchar("client_version", 36).nullable()
    val ip = varchar("ip", 39).nullable()

    val sid = char("sid", 120).nullable()
}