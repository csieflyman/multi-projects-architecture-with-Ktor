/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth.login.logging

import fanpoll.infra.auth.login.LoginResultCode
import fanpoll.infra.database.custom.principalSource
import fanpoll.infra.database.sql.UUIDTable
import fanpoll.infra.database.sql.transaction
import fanpoll.infra.logging.LogEntity
import fanpoll.infra.logging.writers.LogWriter
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

class LoginLogDBWriter : LogWriter {

    override fun write(logEntity: LogEntity) {
        val loginLog = logEntity as LoginLog
        transaction {
            LoginLogTable.insert {
                it[userId] = loginLog.userId
                it[resultCode] = loginLog.resultCode
                it[occurAt] = loginLog.occurAt
                it[project] = loginLog.project
                it[sourceId] = loginLog.source
                it[tenantId] = loginLog.tenantId?.value
                it[clientId] = loginLog.clientId
                it[clientVersion] = loginLog.clientVersion
                it[ip] = loginLog.ip
                it[sid] = loginLog.sid
            }
        }
    }
}

object LoginLogTable : UUIDTable(name = "infra_login_log") {

    val userId = uuid("user_id")
    val resultCode = enumerationByName("result_code", 20, LoginResultCode::class)
    val occurAt = timestamp("occur_at")

    val project = varchar("project", 20)
    val sourceId = principalSource("source") // name "source" conflict
    val tenantId = varchar("tenant_id", 20).nullable()
    val clientId = varchar("client_id", 36).nullable()
    val clientVersion = varchar("client_version", 36).nullable()
    val ip = varchar("ip", 39).nullable()

    val sid = char("sid", 120).nullable()

    override val naturalKeys: List<Column<out Any>> = listOf(id)
    override val surrogateKey: Column<EntityID<UUID>> = id
}