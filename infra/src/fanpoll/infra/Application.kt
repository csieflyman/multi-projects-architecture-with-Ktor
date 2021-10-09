/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra

import fanpoll.infra.app.AppFeature
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
import fanpoll.infra.cache.CacheFeature
import fanpoll.infra.database.DatabaseFeature
import fanpoll.infra.logging.LoggingConfig
import fanpoll.infra.logging.LoggingFeature
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.notification.NotificationFeature
import fanpoll.infra.openapi.OpenApiFeature
import fanpoll.infra.redis.RedisFeature
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.principal
import io.ktor.features.*
import io.ktor.locations.Locations
import io.ktor.serialization.json
import io.ktor.server.engine.ShutDownUrl
import mu.KotlinLogging
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.get
import org.koin.ktor.ext.koin

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

private val logger = KotlinLogging.logger {}

fun Application.main() {

    val appConfig = ApplicationConfigLoader.load()

    // =============== Install Plugins ===============

    install(ContentNegotiation) {
        json(json)
    }

    install(DataConversion, DataConverter)

    install(Locations)

    install(ShutDownUrl.ApplicationCallFeature) {
        shutDownUrl = appConfig.server.shutDownUrl
    }

    install(Authentication)

    install(XForwardedHeaderSupport)

    install(Compression)

    install(Koin) {
        logger(KoinLogger(Level.INFO))

        modules(
            module(createdAtStart = true) {
                single { appConfig }
            },
            koinBaseModule(appConfig)
        )
    }

    install(LoggingFeature)

    install(DatabaseFeature)

    install(RedisFeature)

    install(CacheFeature)

    install(SessionAuthPlugin)

    install(OpenApiFeature)

    install(AppFeature)

    install(NotificationFeature)

    install(StatusPages) {
        val loggingConfig = get<LoggingConfig>()
        val logWriter = get<LogWriter>()
        val responseCreator = get<I18nResponseCreator>()

        exception<Throwable> { cause ->
            val e = ExceptionUtils.wrapException(cause)

            // ASSUMPTION => (1) Principal should be not null
            //  instead of throwing exception when request is unauthenticated (2) need to install DoubleReceive feature
            if (e is InternalServerException ||
                (call.principal<MyPrincipal>() != null && e.code.type.isError())
            ) {
                ExceptionUtils.writeLogToFile(e, call)

                // write to external service
                if (loggingConfig.error.enabled) {
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

    routing()
}