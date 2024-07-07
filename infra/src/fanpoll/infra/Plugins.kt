/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra

import fanpoll.infra.auth.principal.MyPrincipal
import fanpoll.infra.base.exception.ExceptionUtils
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.location.LocationUtils
import fanpoll.infra.base.response.respond
import fanpoll.infra.config.EnvMode
import fanpoll.infra.config.MyApplicationConfig
import fanpoll.infra.i18n.response.I18nResponseCreator
import fanpoll.infra.koin.KoinLogger
import fanpoll.infra.logging.LoggingConfig
import fanpoll.infra.logging.RequestAttributeKey
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.trace.OpenTracingServerPlugin
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.principal
import io.ktor.server.engine.ShutDownUrl
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import io.ktor.server.locations.Locations
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.dataconversion.DataConversion
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import java.util.*

@OptIn(KtorExperimentalLocationsAPI::class)
fun Application.configureBasicPlugins(appConfig: MyApplicationConfig) {
    install(ContentNegotiation) {
        json(fanpoll.infra.base.json.kotlinx.json)
    }

    install(DataConversion, LocationUtils.DataConverter)

    install(Locations)

    install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = appConfig.server.shutDownUrl
        exitCodeSupplier = { 0 }
    }

    install(Authentication)

    install(XForwardedHeaders)

    install(Compression)
}

fun Application.configureKoin(appConfig: MyApplicationConfig) {
    install(Koin) {
        logger(KoinLogger()) // use https://github.com/oshai/kotlin-logging

        modules(
            module(createdAtStart = true) {
                single { appConfig }
                single { ProjectManager() }
            }
        )
    }
}

fun Application.configurePluginsAfterModulesLoaded(appConfig: MyApplicationConfig) {
    configureErrorHandling(appConfig)
    configureTrace(appConfig.infra.logging)
}

private fun Application.configureErrorHandling(appConfig: MyApplicationConfig) {
    val errorLogConfig = appConfig.infra.logging.error
    val application = this
    // StatusPages Plugin depend on LoggingModule
    install(StatusPages) {
        val logWriter = application.get<LogWriter>()
        val responseCreator = application.get<I18nResponseCreator>()

        exception<Throwable> { call, cause ->
            val e = ExceptionUtils.wrapException(cause)

            // ASSUMPTION => (1) Principal should be not null
            //  instead of throwing exception when request is unauthenticated (2) need to install DoubleReceive plugin
            if (e is InternalServerException ||
                (call.principal<MyPrincipal>() != null && e.code.type.isError())
            ) {
                ExceptionUtils.writeLogToFile(e, call)

                // write to external service
                if (errorLogConfig.enabled) {
                    logWriter.write(ErrorLog.request(e, call))
                }
            }
            val errorResponse = responseCreator.createErrorResponse(e, call)
            if (appConfig.server.env == EnvMode.prod)
                errorResponse.clearDetailMessage() // for security concern
            call.respond(errorResponse)
        }
    }
}

private fun Application.configureTrace(loggingConfig: LoggingConfig) {
    val openTracingEnabled = System.getProperty("otel.javaagent.enabled", "true").toBoolean()
    if (openTracingEnabled) {
        install(OpenTracingServerPlugin) {
            filter = { call -> loggingConfig.request.isExcludePath(call) ?: false }
        }
    } else {
        install(CallId) {
            header(RequestAttributeKey.ID.name)
            generate { UUID.randomUUID().toString() }
        }
    }
}