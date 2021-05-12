/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll

import fanpoll.club.ClubProject
import fanpoll.club.club
import fanpoll.infra.ErrorResponseDTO
import fanpoll.infra.ExceptionUtils
import fanpoll.infra.ProjectManager
import fanpoll.infra.app.AppReleaseService
import fanpoll.infra.auth.*
import fanpoll.infra.cache.CacheManager
import fanpoll.infra.controller.LocationsDataConverter
import fanpoll.infra.database.DatabaseManager
import fanpoll.infra.httpclient.HttpClientManager
import fanpoll.infra.logging.*
import fanpoll.infra.login.SessionService
import fanpoll.infra.notification.NotificationSender
import fanpoll.infra.openapi.OpenApi
import fanpoll.infra.openapi.ProjectOpenApiManager
import fanpoll.infra.redis.RedisManager
import fanpoll.infra.utils.json
import fanpoll.ops.OpsProject
import fanpoll.ops.ops
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.principal
import io.ktor.auth.session
import io.ktor.features.*
import io.ktor.http.HttpHeaders
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.serialization.json
import io.ktor.server.engine.ShutDownUrl
import io.ktor.sessions.Sessions
import io.ktor.sessions.header
import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.encodeToString
import mu.KotlinLogging

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

private val logger = KotlinLogging.logger {}

@OptIn(KtorExperimentalAPI::class)
fun Application.module() {

    // ========== Init ==========
    val appConfig = MyApplicationConfig.loadHOCONFile()
    init(appConfig, environment)

    install(XForwardedHeaderSupport)

    install(DoubleReceive) {
        receiveEntireContent = true
    }

    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate(length = 32)
    }

    // COMPATIBILITY => We customized CallLogging feature to MyCallLogging because we want to implement ourself logger for DB or Kinesis
    install(MyCallLogging) {
        config(appConfig.logging)
        mdc(HttpHeaders.XRequestId) { it.callId } // CallId Feature callIdMdc(HttpHeaders.XRequestId)
        mdc(PRINCIPAL_MDC) { call ->
            call.principal<MyPrincipal>()?.let { json.encodeToString(it) }
        }
    }

    install(StatusPages) {
        exception<Throwable> { cause ->
            val e = ExceptionUtils.wrapException(cause)
            val isWriteLog = ExceptionUtils.isWriteLog(call, e)

            // ASSUMPTION => (1) Principal should be not null
            //  instead of throwing exception when request is unauthenticated (2) need to install DoubleReceive feature
            if (isWriteLog) {
                ExceptionUtils.writeLogToFile(e, call)

                // write to external service
                if (appConfig.logging.errorLogEnabled) {
                    val dto = ErrorLogDTO.request(e, call, appConfig.logging)
                    LogManager.write(LogMessage(LogType.SERVER_ERROR, dto))
                }
            }
            call.respond(e.code.httpStatusCode, ErrorResponseDTO(e, call))
        }
    }

    install(Compression)

    install(ContentNegotiation) {
        json(json)
    }

    install(Locations)

    install(DataConversion, LocationsDataConverter)

    install(Authentication) {

        session(SessionAuthConfig.providerName, SessionAuthConfig.configure())

        runAs()

        ops(appConfig.ops.auth)

        club(appConfig.club.auth)
    }

    install(Sessions) {

        header<UserPrincipal>(SessionAuth.SESSION_ID_HEADER_NAME, SessionAuthConfig.cacheSessionStorage) {
            serializer = SessionAuthConfig.jsonSessionSerializer
        }
    }

    install(ShutDownUrl.ApplicationCallFeature) {
        shutDownUrl = appConfig.server.shutDownUrl
        // A function that will be executed to get the exit code of the process
        exitCodeSupplier = { 0 }
    }

    install(OpenApi) {
        openApiConfig = appConfig.openApi
    }

    routing(appConfig)
}

private fun init(appConfig: MyApplicationConfig, environment: ApplicationEnvironment) {
    initServices(appConfig, environment)
    initProjects(appConfig)
}

private fun initServices(appConfig: MyApplicationConfig, environment: ApplicationEnvironment) {
    try {
        LogManager.init(appConfig.logging, appConfig.server)
        DatabaseManager.init(appConfig.database)
        RedisManager.init(appConfig.redis)
        SessionService.init(RedisManager.client, appConfig.redis)
        CacheManager.initBuiltinRedisCaches(RedisManager.client)
        NotificationSender.init(appConfig.notification, appConfig.server.env)

        initHttpClientServices(appConfig)

        AppReleaseService.init()
        ProjectOpenApiManager.init(appConfig.openApi)

        environment.monitor.subscribe(ApplicationStopping) { HttpClientManager.shutdown() }
        environment.monitor.subscribe(ApplicationStopping) { CacheManager.shutdown() }
        environment.monitor.subscribe(ApplicationStopping) { RedisManager.shutdown() }
        environment.monitor.subscribe(ApplicationStopping) { DatabaseManager.shutdown() }
        environment.monitor.subscribe(ApplicationStopping) { LogManager.shutdown() }
    } catch (cause: Throwable) {
        val e = ExceptionUtils.wrapException(cause)
        ExceptionUtils.writeLogToFile(e)
        throw e
    }
}

private fun initHttpClientServices(appConfig: MyApplicationConfig) {

}

private fun initProjects(appConfig: MyApplicationConfig) {
    try {
        ProjectManager.init(appConfig)
        ProjectManager.register(OpsProject)
        ProjectManager.register(ClubProject)
    } catch (cause: Throwable) {
        val e = ExceptionUtils.wrapException(cause)
        ExceptionUtils.writeLogToFile(e)
        throw e
    }
}