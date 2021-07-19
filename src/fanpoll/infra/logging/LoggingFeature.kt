/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.MyApplicationConfig
import fanpoll.infra.auth.principal.MyPrincipal
import fanpoll.infra.base.async.AsyncExecutorConfig
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.json.json
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.error.ErrorLogConfig
import fanpoll.infra.logging.error.ErrorLogDBWriter
import fanpoll.infra.logging.request.MyCallLoggingFeature
import fanpoll.infra.logging.request.RequestLog
import fanpoll.infra.logging.request.RequestLogConfig
import fanpoll.infra.logging.request.RequestLogDBWriter
import fanpoll.infra.logging.writers.*
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.application.ApplicationStopped
import io.ktor.application.install
import io.ktor.auth.principal
import io.ktor.features.CallId
import io.ktor.features.DoubleReceive
import io.ktor.features.callId
import io.ktor.features.generate
import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey
import kotlinx.serialization.encodeToString
import mu.KotlinLogging
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.koin

class LoggingFeature(configuration: Configuration) {

    class Configuration {

        private lateinit var request: RequestLogConfig
        private lateinit var error: ErrorLogConfig
        private var writer: LogWriterConfig? = null
        private var asyncExecutor: AsyncExecutorConfig? = null

        fun request(block: RequestLogConfig.Builder.() -> Unit) {
            request = RequestLogConfig.Builder().apply(block).build()
        }

        fun error(block: ErrorLogConfig.Builder.() -> Unit) {
            error = ErrorLogConfig.Builder().apply(block).build()
        }

        fun writer(block: LogWriterConfig.Builder.() -> Unit) {
            writer = LogWriterConfig.Builder().apply(block).build()
        }

        fun asyncExecutor(configure: AsyncExecutorConfig.Builder.() -> Unit) {
            asyncExecutor = AsyncExecutorConfig.Builder().apply(configure).build()
        }

        fun build(): LoggingConfig {
            return LoggingConfig(request, error, writer, asyncExecutor)
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, LoggingFeature> {

        override val key = AttributeKey<LoggingFeature>("Logging")

        private val logger = KotlinLogging.logger {}

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): LoggingFeature {
            val configuration = Configuration().apply(configure)
            val feature = LoggingFeature(configuration)

            val appConfig = pipeline.get<MyApplicationConfig>()
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

                        val awsKinesisLogWriter = loggingConfig.writer?.awsAwsKinesis?.let {
                            AwsKinesisLogWriter(it, serverConfig)
                        }
                        if (awsKinesisLogWriter != null)
                            single { awsKinesisLogWriter }

                        if (loggingConfig.request.enabled) {
                            val requestLogWriter = when (loggingConfig.request.destination) {
                                LogDestination.File -> fileLogWriter
                                LogDestination.Database -> RequestLogDBWriter()
                                LogDestination.AwsKinesis -> awsKinesisLogWriter ?: throw InternalServerException(
                                    ResponseCode.SERVER_CONFIG_ERROR, "AwsKinesisLogWriter is not configured"
                                )
                            }
                            logMessageDispatcher.register(RequestLog.LOG_TYPE, requestLogWriter)
                        }
                        if (loggingConfig.error.enabled) {
                            val errorLogWriter = when (loggingConfig.error.destination) {
                                LogDestination.File -> fileLogWriter
                                LogDestination.Database -> ErrorLogDBWriter()
                                LogDestination.AwsKinesis -> awsKinesisLogWriter ?: throw InternalServerException(
                                    ResponseCode.SERVER_CONFIG_ERROR, "kinesisLogWriter is not configured"
                                )
                            }
                            logMessageDispatcher.register(ErrorLog.LOG_TYPE, errorLogWriter)
                        }

                        val logWriter = loggingConfig.asyncExecutor?.let {
                            LogMessageCoroutineActor(it.coroutineActor, logMessageDispatcher)
                        } ?: logMessageDispatcher

                        single { logWriter }

                        pipeline.environment.monitor.subscribe(ApplicationStopped) { logWriter.shutdown() }
                    }
                )
            }

            pipeline.install(DoubleReceive) {
                receiveEntireContent = true
            }

            pipeline.install(CallId) {
                header(HttpHeaders.XRequestId)
                generate(length = 32)
            }

            // customize Ktor CallLogging feature
            pipeline.install(MyCallLoggingFeature) {
                mdc(HttpHeaders.XRequestId) { it.callId } // CallId Feature callIdMdc(HttpHeaders.XRequestId)
                mdc(MyPrincipal.MDC) { call ->
                    call.principal<MyPrincipal>()?.let { json.encodeToString(it) }
                }
            }

            return feature
        }
    }
}

enum class LogDestination {
    File, Database, AwsKinesis
}

data class LoggingConfig(
    val request: RequestLogConfig,
    val error: ErrorLogConfig,
    val writer: LogWriterConfig? = null,
    val asyncExecutor: AsyncExecutorConfig? = null
)

data class LogWriterConfig(
    val awsAwsKinesis: AwsKinesisConfig? = null
) {

    class Builder {

        private var awsKinesis: AwsKinesisConfig? = null

        fun awsKinesis(block: AwsKinesisConfig.Builder.() -> Unit) {
            awsKinesis = AwsKinesisConfig.Builder().apply(block).build()
        }

        fun build(): LogWriterConfig {
            return LogWriterConfig(awsKinesis)
        }
    }
}