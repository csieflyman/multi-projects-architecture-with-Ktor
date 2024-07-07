/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.koin.KoinApplicationShutdownManager
import fanpoll.infra.logging.error.loadErrorLogModule
import fanpoll.infra.logging.request.loadRequestLogModule
import fanpoll.infra.logging.writers.FileLogWriter
import fanpoll.infra.logging.writers.LogEntityCoroutineActor
import fanpoll.infra.logging.writers.LogEntityDispatcher
import io.ktor.server.application.Application
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module

fun Application.loadLoggingModule(loggingConfig: LoggingConfig) {
    loadKoinModules(module(createdAtStart = true) {
        initLogWriter(loggingConfig)
    })

    if (loggingConfig.request.enabled) {
        loadRequestLogModule(loggingConfig)
    }

    if (loggingConfig.error.enabled) {
        loadErrorLogModule(loggingConfig)
    }
}

private fun Module.initLogWriter(loggingConfig: LoggingConfig) {
    val fileLogWriter = FileLogWriter()
    single { fileLogWriter }

    val logEntityDispatcher = LogEntityDispatcher(fileLogWriter)
    single { logEntityDispatcher }

    val logWriter = loggingConfig.asyncExecutor?.let {
        LogEntityCoroutineActor(it.coroutineActor, logEntityDispatcher)
    } ?: logEntityDispatcher
    single { logWriter }

    KoinApplicationShutdownManager.register { logWriter.shutdown() }
}
