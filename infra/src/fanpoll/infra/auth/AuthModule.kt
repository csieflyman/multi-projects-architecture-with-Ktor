/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.auth

import fanpoll.infra.auth.login.logging.LoginLog
import fanpoll.infra.auth.login.logging.LoginLogDBWriter
import fanpoll.infra.auth.login.logging.LoginLogDTO
import fanpoll.infra.auth.login.logging.LoginLogTable
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.exposed.util.ResultRowMapper
import fanpoll.infra.database.exposed.util.ResultRowMappers
import fanpoll.infra.logging.LogDestination
import fanpoll.infra.logging.writers.FileLogWriter
import fanpoll.infra.logging.writers.LogEntityDispatcher
import fanpoll.infra.logging.writers.LokiLogWriter
import fanpoll.infra.session.MySessionStorage
import fanpoll.infra.session.UserSession
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionSerializer
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.header
import kotlinx.serialization.encodeToString
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.ktor.ext.get

fun Application.loadAuthModule(sessionAuthConfig: SessionAuthConfig) = loadKoinModules(module(createdAtStart = true) {
    val sessionStorage = get<MySessionStorage>()
    installSessionsPlugin(sessionStorage)

    initLogWriter(sessionAuthConfig.logging.destination)

    registerResultRowMappers()
})

private fun Application.installSessionsPlugin(sessionStorage: MySessionStorage) {
    install(Sessions) {
        header<UserSession>(AuthConst.SESSION_ID_HEADER_NAME, sessionStorage) {
            serializer = object : SessionSerializer<UserSession> {

                override fun deserialize(text: String): UserSession =
                    json.decodeFromString(text)

                override fun serialize(session: UserSession): String =
                    json.encodeToString(session)
            }
        }
    }
}

private fun Application.initLogWriter(logDestination: LogDestination) {
    val loginLogWriter = when (logDestination) {
        LogDestination.File -> get<FileLogWriter>()
        LogDestination.Database -> LoginLogDBWriter()
        LogDestination.Loki -> get<LokiLogWriter>()
        else -> throw InternalServerException(
            InfraResponseCode.SERVER_CONFIG_ERROR, "$logDestination is invalid"
        )
    }
    val logEntityDispatcher = get<LogEntityDispatcher>()
    logEntityDispatcher.register(LoginLog.LOG_TYPE, loginLogWriter)
}

private fun registerResultRowMappers() {
    ResultRowMappers.register(
        ResultRowMapper(LoginLogDTO::class, LoginLogTable)
    )
}