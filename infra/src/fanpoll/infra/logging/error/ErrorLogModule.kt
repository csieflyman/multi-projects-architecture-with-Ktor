/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.error

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.exposed.InfraDatabase
import fanpoll.infra.database.exposed.util.ResultRowMapper
import fanpoll.infra.database.exposed.util.ResultRowMappers
import fanpoll.infra.logging.LogDestination
import fanpoll.infra.logging.LoggingConfig
import fanpoll.infra.logging.writers.FileLogWriter
import fanpoll.infra.logging.writers.LogEntityDispatcher
import fanpoll.infra.logging.writers.SentryLogWriter
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.ktor.ext.get

fun Application.loadErrorLogModule(loggingConfig: LoggingConfig) {
    initLogWriter(loggingConfig)
    registerResultRowMappers()
}

private fun Application.initLogWriter(loggingConfig: LoggingConfig) {
    val errorLogWriter = when (loggingConfig.error.destination) {
        LogDestination.File -> get<FileLogWriter>()
        LogDestination.Database -> ErrorLogDBWriter(get<Database>(named(InfraDatabase.Infra.name)))
        LogDestination.Sentry -> {
            loggingConfig.writers?.sentry?.let {
                SentryLogWriter(it, loggingConfig.appInfo, loggingConfig.server)
            } ?: throw InternalServerException(
                InfraResponseCode.SERVER_CONFIG_ERROR, "SentryLogWriter is not configured"
            )
        }

        else -> throw InternalServerException(
            InfraResponseCode.SERVER_CONFIG_ERROR, "ErrorLogWriter is not configured"
        )
    }
    val logEntityDispatcher = get<LogEntityDispatcher>()
    logEntityDispatcher.register(ErrorLog.LOG_TYPE, errorLogWriter)
}

private fun registerResultRowMappers() {
    ResultRowMappers.register(
        ResultRowMapper(ErrorLogDTO::class, ErrorLogTable)
    )
}