/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra

import fanpoll.infra.app.AppPlugin
import fanpoll.infra.auth.SessionAuthPlugin
import fanpoll.infra.auth.principal.MyPrincipal
import fanpoll.infra.base.exception.ExceptionUtils
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.json.json
import fanpoll.infra.base.koin.KoinApplicationShutdownManager
import fanpoll.infra.base.koin.KoinLogger
import fanpoll.infra.base.koinBaseModule
import fanpoll.infra.base.location.LocationUtils.DataConverter
import fanpoll.infra.base.response.I18nResponseCreator
import fanpoll.infra.base.response.respond
import fanpoll.infra.cache.CachePlugin
import fanpoll.infra.database.DatabasePlugin
import fanpoll.infra.logging.LoggingConfig
import fanpoll.infra.logging.LoggingPlugin
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.notification.NotificationPlugin
import fanpoll.infra.openapi.OpenApiPlugin
import fanpoll.infra.redis.RedisPlugin
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.principal
import io.ktor.server.engine.ShutDownUrl
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import io.ktor.server.locations.Locations
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.dataconversion.DataConversion
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import org.koin.ktor.plugin.koin

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

private val logger = KotlinLogging.logger {}

@OptIn(KtorExperimentalLocationsAPI::class)
fun Application.main(configureAppConfig: (MyApplicationConfig.() -> Unit)? = null) {

    val appConfig = ApplicationConfigLoader.load()

    if (configureAppConfig != null) {
        configureAppConfig(appConfig)
    }

    // =============== Install Plugins ===============

    install(ContentNegotiation) {
        json(json)
    }

    install(DataConversion, DataConverter)

    install(Locations)

    install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = appConfig.server.shutDownUrl
        exitCodeSupplier = { 0 }
    }

    install(Authentication)

    install(XForwardedHeaders)

    install(Compression)

    install(Koin) {
        logger(KoinLogger()) // use https://github.com/oshai/kotlin-logging

        modules(
            module(createdAtStart = true) {
                single { appConfig }
            },
            koinBaseModule(appConfig)
        )
    }

    install(LoggingPlugin)

    install(DatabasePlugin)

    install(RedisPlugin)

    install(CachePlugin)

    install(SessionAuthPlugin)

    install(OpenApiPlugin)

    install(AppPlugin)

    install(NotificationPlugin)

    install(StatusPages) {
        val loggingConfig = this@main.get<LoggingConfig>()
        val logWriter = this@main.get<LogWriter>()
        val responseCreator = this@main.get<I18nResponseCreator>()

        exception<Throwable> { call, cause ->
            val e = ExceptionUtils.wrapException(cause)

            // ASSUMPTION => (1) Principal should be not null
            //  instead of throwing exception when request is unauthenticated (2) need to install DoubleReceive plugin
            if (e is InternalServerException ||
                (call.principal<MyPrincipal>() != null && e.code.type.isError())
            ) {
                ExceptionUtils.writeLogToFile(e, call)

                // write to external service
                if (loggingConfig.error != null && loggingConfig.error.enabled) {
                    logWriter.write(ErrorLog.request(e, call))
                }
            }
            val errorResponse = responseCreator.createErrorResponse(e, call)
            call.respond(errorResponse)
        }
    }

    // =============== End Plugin Installation ===============

    koin {
        modules(
            module(createdAtStart = true) {
                single { ProjectManager(get()) }
            }
        )
    }

    KoinApplicationShutdownManager.complete(environment)

    staticContentRouting()
}