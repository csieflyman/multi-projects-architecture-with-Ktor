/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.MyApplicationConfig
import fanpoll.infra.auth.ClientVersionAttributeKey
import fanpoll.infra.auth.principal.MyPrincipal
import fanpoll.infra.base.async.AsyncExecutorConfig
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.koin.KoinApplicationShutdownManager
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.error.ErrorLogConfig
import fanpoll.infra.logging.error.ErrorLogDBWriter
import fanpoll.infra.logging.otel.OpenTracingServer
import fanpoll.infra.logging.request.MyCallLoggingFeature
import fanpoll.infra.logging.request.RequestLog
import fanpoll.infra.logging.request.RequestLogConfig
import fanpoll.infra.logging.request.RequestLogDBWriter
import fanpoll.infra.logging.writers.*
import io.ktor.application.*
import io.ktor.auth.principal
import io.ktor.features.CallId
import io.ktor.features.DoubleReceive
import io.ktor.features.callId
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.response.ApplicationSendPipeline
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import mu.KotlinLogging
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.koin
import java.time.Instant
import java.util.*

class LoggingFeature(configuration: Configuration) {

    class Configuration {

        private lateinit var request: RequestLogConfig
        private lateinit var error: ErrorLogConfig
        private var asyncExecutor: AsyncExecutorConfig? = null
        private var writers: LoggingWritersConfig? = null

        fun request(block: RequestLogConfig.Builder.() -> Unit) {
            request = RequestLogConfig.Builder().apply(block).build()
        }

        fun error(block: ErrorLogConfig.Builder.() -> Unit) {
            error = ErrorLogConfig.Builder().apply(block).build()
        }

        fun asyncExecutor(configure: AsyncExecutorConfig.Builder.() -> Unit) {
            asyncExecutor = AsyncExecutorConfig.Builder().apply(configure).build()
        }

        fun writers(configure: LoggingWritersConfig.Builder.() -> Unit) {
            writers = LoggingWritersConfig.Builder().apply(configure).build()
        }

        fun build(): LoggingConfig {
            return LoggingConfig(request, error, asyncExecutor, writers)
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

                        val logEntityDispatcher = LogEntityDispatcher(FileLogWriter())
                        single { logEntityDispatcher }

                        val lokiLogWriter = loggingConfig.writers?.loki?.let {
                            LokiLogWriter(it, serverConfig)
                        }
                        if (lokiLogWriter != null)
                            single { lokiLogWriter }

                        if (loggingConfig.request != null && loggingConfig.request.enabled) {
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
                            logEntityDispatcher.register(RequestLog.LOG_TYPE, requestLogWriter)
                        }

                        val sentryLogWriter = loggingConfig.writers?.sentry?.let {
                            SentryLogWriter(it, appInfoConfig, serverConfig)
                        }
                        if (sentryLogWriter != null)
                            single { sentryLogWriter }

                        if (loggingConfig.error != null && loggingConfig.error.enabled) {
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
                            logEntityDispatcher.register(ErrorLog.LOG_TYPE, errorLogWriter)
                        }

                        val logWriter = loggingConfig.asyncExecutor?.let {
                            LogEntityCoroutineActor(it.coroutineActor, logEntityDispatcher)
                        } ?: logEntityDispatcher

                        single { logWriter }

                        KoinApplicationShutdownManager.register { logWriter.shutdown() }
                    }
                )
            }

            pipeline.install(DoubleReceive) {
                receiveEntireContent = true
            }

            val openTracingEnabled = System.getProperty("otel.javaagent.enabled", "true").toBoolean()
            if (openTracingEnabled) {
                pipeline.install(OpenTracingServer) {
                    filter = { call -> loggingConfig.request?.isExcludePath(call) ?: false }
                }
            } else {
                pipeline.install(CallId) {
                    header(RequestAttributeKey.ID.name)
                    generate { UUID.randomUUID().toString() }
                }
            }

            setRequestAttributes(pipeline)

            // customize Ktor CallLogging feature
            if (loggingConfig.request != null && loggingConfig.request.enabled) {
                installMyCallLogging(pipeline, loggingConfig.request)
            }

            return feature
        }

        private fun installMyCallLogging(
            pipeline: Application,
            config: RequestLogConfig
        ) {
            pipeline.install(MyCallLoggingFeature) {
                mdc(RequestAttributeKey.TRACE_ID.name) { it.attributes.getOrNull(RequestAttributeKey.TRACE_ID) }
                mdc(RequestAttributeKey.ID.name) { it.attributes[RequestAttributeKey.ID] }

                filter { call ->
                    when {
                        call.request.httpMethod == HttpMethod.Get && !config.includeGetMethod -> false
                        config.isExcludePath(call) -> false
                        call.principal<MyPrincipal>() == null -> false // ASSUMPTION => only logging authenticated request
                        else -> true
                    }
                }

                val logWriter = pipeline.get<LogWriter>()
                writeLog = { call ->
                    logWriter.write(RequestLog(config, call))
                }
            }

            if (config.includeResponseBody) {
                val responseBodyAttributePhase = PipelinePhase("ResponseBodyAttribute")
                pipeline.sendPipeline.insertPhaseAfter(ApplicationSendPipeline.Render, responseBodyAttributePhase)
                pipeline.sendPipeline.intercept(responseBodyAttributePhase) { message ->
                    if (message is TextContent) {
                        call.attributes.put(ResponseAttributeKey.BODY, message.text)
                    }
                }
            }
        }

        private fun setRequestAttributes(pipeline: Application) {
            val requestAttributePhase = PipelinePhase("RequestAttribute")
            pipeline.insertPhaseBefore(ApplicationCallPipeline.Monitoring, requestAttributePhase)
            pipeline.intercept(requestAttributePhase) {
                if (!call.attributes.contains(RequestAttributeKey.ID)) {
                    call.attributes.put(RequestAttributeKey.ID, call.callId ?: UUID.randomUUID().toString())
                }

                call.attributes.put(RequestAttributeKey.AT, Instant.now())

                if (call.request.headers.contains(ClientVersionAttributeKey.CLIENT_VERSION.name))
                    call.attributes.put(
                        ClientVersionAttributeKey.CLIENT_VERSION,
                        call.request.header(ClientVersionAttributeKey.CLIENT_VERSION.name)!!
                    )
            }
        }
    }
}

object RequestAttributeKey {

    val TRACE_ID = AttributeKey<String>("traceId") // optional
    val ID = AttributeKey<String>(HttpHeaders.XRequestId)
    val AT = AttributeKey<Instant>("reqAt")
    val TAGS = AttributeKey<Map<String, String>>("tags") // optional
}

object ResponseAttributeKey {

    val BODY = AttributeKey<String>("rspBody") // optional
}

enum class LogDestination {
    File, Database, Loki, Sentry
}

data class LoggingConfig(
    val request: RequestLogConfig? = null,
    val error: ErrorLogConfig? = null,
    val asyncExecutor: AsyncExecutorConfig? = null,
    val writers: LoggingWritersConfig? = null
)

data class LoggingWritersConfig(
    val loki: LokiConfig? = null,
    val sentry: SentryConfig? = null
) {
    class Builder {

        private var loki: LokiConfig? = null
        private var sentry: SentryConfig? = null

        fun loki(block: LokiConfig.Builder.() -> Unit) {
            loki = LokiConfig.Builder().apply(block).build()
        }

        fun sentry(block: SentryConfig.Builder.() -> Unit) {
            sentry = SentryConfig.Builder().apply(block).build()
        }

        fun build(): LoggingWritersConfig {
            return LoggingWritersConfig(loki, sentry)
        }
    }
}