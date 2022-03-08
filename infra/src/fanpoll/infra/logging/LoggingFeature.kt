/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.MyApplicationConfig
import fanpoll.infra.base.async.AsyncExecutorConfig
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.koin.KoinApplicationShutdownManager
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.error.ErrorLogConfig
import fanpoll.infra.logging.error.ErrorLogDBWriter
import fanpoll.infra.logging.error.SentryLogWriter
import fanpoll.infra.logging.request.*
import fanpoll.infra.logging.writers.FileLogWriter
import fanpoll.infra.logging.writers.LogMessageCoroutineActor
import fanpoll.infra.logging.writers.LogMessageDispatcher
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.application.install
import io.ktor.features.CallId
import io.ktor.features.DoubleReceive
import io.ktor.features.callId
import io.ktor.util.AttributeKey
import mu.KotlinLogging
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.koin
import java.util.*

class LoggingFeature(configuration: Configuration) {

    class Configuration {

        private lateinit var request: RequestLogConfig
        private lateinit var error: ErrorLogConfig
        private var asyncExecutor: AsyncExecutorConfig? = null

        fun request(block: RequestLogConfig.Builder.() -> Unit) {
            request = RequestLogConfig.Builder().apply(block).build()
        }

        fun error(block: ErrorLogConfig.Builder.() -> Unit) {
            error = ErrorLogConfig.Builder().apply(block).build()
        }

        fun asyncExecutor(configure: AsyncExecutorConfig.Builder.() -> Unit) {
            asyncExecutor = AsyncExecutorConfig.Builder().apply(configure).build()
        }

        fun build(): LoggingConfig {
            return LoggingConfig(request, error, asyncExecutor)
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, LoggingFeature> {

        override val key = AttributeKey<LoggingFeature>("Logging")

        private val logger = KotlinLogging.logger {}

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): LoggingFeature {
            val configuration = Configuration().apply(configure)
            val feature = LoggingFeature(configuration)

            val appConfig = pipeline.get<MyApplicationConfig>()
            val appInfoConfig = appConfig.info
            val serverConfig = appConfig.server
            val loggingConfig = appConfig.infra.logging ?: configuration.build()

            pipeline.koin {
                modules(
                    module(createdAtStart = true) {
                        single { loggingConfig }

                        val fileLogWriter = FileLogWriter()
                        single { fileLogWriter }

                        val logMessageDispatcher = LogMessageDispatcher(FileLogWriter())
                        single { logMessageDispatcher }

                        val lokiLogWriter = loggingConfig.request.loki?.let {
                            LokiLogWriter(it, serverConfig)
                        }
                        if (lokiLogWriter != null)
                            single { lokiLogWriter }

                        if (loggingConfig.request.enabled) {
                            val requestLogWriter = when (loggingConfig.request.destination) {
                                LogDestination.File -> fileLogWriter
                                LogDestination.Database -> RequestLogDBWriter()
                                LogDestination.Loki -> lokiLogWriter ?: throw InternalServerException(
                                    InfraResponseCode.SERVER_CONFIG_ERROR, "LokiLogWriter is not configured"
                                )
                                else -> throw InternalServerException(
                                    InfraResponseCode.SERVER_CONFIG_ERROR, "RequestLogWriter is not configured"
                                )
                            }
                            logMessageDispatcher.register(RequestLog.LOG_TYPE, requestLogWriter)
                        }

                        val sentryLogWriter = loggingConfig.error.sentry?.let {
                            SentryLogWriter(it, appInfoConfig, serverConfig)
                        }
                        if (sentryLogWriter != null)
                            single { sentryLogWriter }

                        if (loggingConfig.error.enabled) {
                            val errorLogWriter = when (loggingConfig.error.destination) {
                                LogDestination.File -> fileLogWriter
                                LogDestination.Database -> ErrorLogDBWriter()
                                LogDestination.Sentry -> sentryLogWriter ?: throw InternalServerException(
                                    InfraResponseCode.SERVER_CONFIG_ERROR, "SentryLogWriter is not configured"
                                )
                                else -> throw InternalServerException(
                                    InfraResponseCode.SERVER_CONFIG_ERROR, "ErrorLogWriter is not configured"
                                )
                            }
                            logMessageDispatcher.register(ErrorLog.LOG_TYPE, errorLogWriter)
                        }

                        val logWriter = loggingConfig.asyncExecutor?.let {
                            LogMessageCoroutineActor(it.coroutineActor, logMessageDispatcher)
                        } ?: logMessageDispatcher

                        single { logWriter }

                        KoinApplicationShutdownManager.register { logWriter.shutdown() }
                    }
                )
            }

            pipeline.install(DoubleReceive) {
                receiveEntireContent = true
            }

            pipeline.install(CallId) {
                header(RequestAttribute.TRACE_ID.name)
                generate { UUID.randomUUID().toString() }
            }

            // customize Ktor CallLogging feature
            pipeline.install(MyCallLoggingFeature) {
                mdc(RequestAttribute.TRACE_ID.name) { it.callId }
                mdc(RequestAttribute.REQ_ID.name) { it.attributes[RequestAttribute.REQ_ID].toString() }
                mdc(RequestAttribute.PARENT_REQ_ID.name) { it.attributes.getOrNull(RequestAttribute.PARENT_REQ_ID)?.toString() }
            }

            return feature
        }
    }
}

enum class LogDestination {
    File, Database, Loki, Sentry
}

data class LoggingConfig(
    val request: RequestLogConfig,
    val error: ErrorLogConfig,
    val asyncExecutor: AsyncExecutorConfig? = null
)