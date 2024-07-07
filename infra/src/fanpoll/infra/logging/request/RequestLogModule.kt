/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.logging.request

import fanpoll.infra.auth.principal.ClientAttributeKey
import fanpoll.infra.auth.principal.MyPrincipal
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.exposed.InfraDatabase
import fanpoll.infra.database.exposed.util.ResultRowMapper
import fanpoll.infra.database.exposed.util.ResultRowMappers
import fanpoll.infra.logging.LogDestination
import fanpoll.infra.logging.LoggingConfig
import fanpoll.infra.logging.RequestAttributeKey
import fanpoll.infra.logging.ResponseAttributeKey
import fanpoll.infra.logging.writers.FileLogWriter
import fanpoll.infra.logging.writers.LogEntityDispatcher
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.logging.writers.LokiLogWriter
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.principal
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.response.ApplicationSendPipeline
import io.ktor.util.pipeline.PipelinePhase
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.get
import java.time.Instant
import java.util.*

fun Application.loadRequestLogModule(loggingConfig: LoggingConfig) {
    initLogWriters(loggingConfig)

    // customize Ktor CallLogging plugin
    installMyCallLoggingPlugin(loggingConfig.request)

    setRequestAttributes()

    install(DoubleReceive)

    registerResultRowMappers()
}

private fun Application.initLogWriters(loggingConfig: LoggingConfig) {
    val requestLogWriter = when (loggingConfig.request.destination) {
        LogDestination.File -> get<FileLogWriter>()
        LogDestination.Database -> RequestLogDBWriter(get<Database>(named(InfraDatabase.Infra.name)))
        LogDestination.Loki -> {
            val lokiLogWriter = loggingConfig.writers?.loki?.let {
                LokiLogWriter(it, loggingConfig.server)
            }
            lokiLogWriter?.apply {
                loadKoinModules(module(createdAtStart = true) {
                    single { lokiLogWriter }
                })
            } ?: throw InternalServerException(
                InfraResponseCode.SERVER_CONFIG_ERROR, "LokiLogWriter is not configured"
            )
        }

        else -> throw InternalServerException(
            InfraResponseCode.SERVER_CONFIG_ERROR, "RequestLogWriter is not configured"
        )
    }
    val logEntityDispatcher = get<LogEntityDispatcher>()
    logEntityDispatcher.register(RequestLog.LOG_TYPE, requestLogWriter)
}

private fun Application.installMyCallLoggingPlugin(
    config: RequestLogConfig
) {
    install(MyCallLoggingPlugin) {
        attributes.getOrNull(RequestAttributeKey.TRACE_ID)?.let { traceId -> mdc(RequestAttributeKey.TRACE_ID.name) { traceId } }
        attributes.getOrNull(RequestAttributeKey.ID)?.let { requestId -> mdc(RequestAttributeKey.ID.name) { requestId } }

        filter { call ->
            when {
                call.request.httpMethod == HttpMethod.Get && !config.includeGetMethod -> false
                config.isExcludePath(call) -> false
                call.principal<MyPrincipal>() == null -> false // ASSUMPTION => only logging authenticated request
                else -> true
            }
        }

        val logWriter = get<LogWriter>()
        writeLog = { call ->
            runBlocking {
                logWriter.write(RequestLog(config, call))
            }
        }
    }

    if (config.includeResponseBody) {
        val responseBodyAttributePhase = PipelinePhase("ResponseBodyAttribute")
        sendPipeline.insertPhaseAfter(ApplicationSendPipeline.Render, responseBodyAttributePhase)
        sendPipeline.intercept(responseBodyAttributePhase) { message ->
            if (message is TextContent) {
                call.attributes.put(ResponseAttributeKey.BODY, message.text)
            }
        }
    }
}

private fun Application.setRequestAttributes() {
    val requestAttributePhase = PipelinePhase("RequestAttribute")
    insertPhaseBefore(ApplicationCallPipeline.Monitoring, requestAttributePhase)
    intercept(requestAttributePhase) {
        if (!call.attributes.contains(RequestAttributeKey.ID)) {
            call.attributes.put(RequestAttributeKey.ID, call.callId ?: UUID.randomUUID().toString())
        }

        call.attributes.put(RequestAttributeKey.AT, Instant.now())

        if (call.request.headers.contains(ClientAttributeKey.CLIENT_VERSION.name))
            call.attributes.put(
                ClientAttributeKey.CLIENT_VERSION,
                call.request.header(ClientAttributeKey.CLIENT_VERSION.name)!!
            )
    }
}

private fun registerResultRowMappers() {
    ResultRowMappers.register(
        ResultRowMapper(RequestLogDTO::class, RequestLogTable)
    )
}