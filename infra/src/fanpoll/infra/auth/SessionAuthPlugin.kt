/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth

import fanpoll.infra.MyApplicationConfig
import fanpoll.infra.auth.login.LoginService
import fanpoll.infra.auth.login.logging.LoginLog
import fanpoll.infra.auth.login.logging.LoginLogConfig
import fanpoll.infra.auth.login.logging.LoginLogDBWriter
import fanpoll.infra.auth.login.session.*
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.base.config.ValidateableConfig
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.json.json
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.util.DBAsyncTaskCoroutineActor
import fanpoll.infra.logging.LogDestination
import fanpoll.infra.logging.writers.FileLogWriter
import fanpoll.infra.logging.writers.LogEntityDispatcher
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.logging.writers.LokiLogWriter
import fanpoll.infra.redis.RedisKeyspaceNotificationListener
import fanpoll.infra.redis.ktorio.RedisClient
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.application.install
import io.ktor.sessions.SessionSerializer
import io.ktor.sessions.Sessions
import io.ktor.sessions.header
import io.ktor.util.AttributeKey
import mu.KotlinLogging
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.koin

class SessionAuthPlugin(configuration: Configuration) {

    class Configuration {

        lateinit var storageType: SessionStorageType
        private lateinit var session: SessionConfig
        private lateinit var logging: LoginLogConfig
        var redisKeyExpiredNotification: Boolean? = null

        fun session(configure: SessionConfig.Builder.() -> Unit) {
            session = SessionConfig.Builder().apply(configure).build().apply { validate() }
        }

        fun logging(configure: LoginLogConfig.Builder.() -> Unit) {
            logging = LoginLogConfig.Builder().apply(configure).build()
        }

        fun build(): SessionAuthConfig {
            return SessionAuthConfig(storageType, session, logging, redisKeyExpiredNotification).apply { validate() }
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, SessionAuthPlugin> {

        override val key = AttributeKey<SessionAuthPlugin>("MySession")

        private val logger = KotlinLogging.logger {}

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): SessionAuthPlugin {
            val configuration = Configuration().apply(configure)
            val feature = SessionAuthPlugin(configuration)

            val appConfig = pipeline.get<MyApplicationConfig>()
            val sessionAuthConfig = appConfig.infra.sessionAuth ?: configuration.build()

            // ========== SessionStorage ==========

            val sessionStorage = when (sessionAuthConfig.storageType) {
                SessionStorageType.Redis -> {
                    val redisClient = pipeline.get<RedisClient>()
                    val redisKeyspaceNotificationListener = if (sessionAuthConfig.redisKeyExpiredNotification == true) {
                        pipeline.get<RedisKeyspaceNotificationListener>()
                    } else null
                    val logWriter = pipeline.get<LogWriter>()
                    RedisSessionStorage(sessionAuthConfig.session, redisClient, redisKeyspaceNotificationListener, logWriter)
                }
            }

            pipeline.koin {
                modules(
                    module(createdAtStart = true) {
                        single<MySessionStorage> { sessionStorage }
                    }
                )
            }

            pipeline.install(Sessions) {
                header<UserPrincipal>(AuthConst.SESSION_ID_HEADER_NAME, sessionStorage) {
                    serializer = object : SessionSerializer<UserPrincipal> {

                        override fun deserialize(text: String): UserPrincipal =
                            json.decodeFromString(UserSession.Value.serializer(), text).principal()

                        override fun serialize(session: UserPrincipal): String =
                            json.encodeToString(UserSession.Value.serializer(), session.session!!.value)
                    }
                }
            }

            // ========== LoginService ==========

            val loginLogWriter = when (sessionAuthConfig.logging.destination) {
                LogDestination.File -> pipeline.get<FileLogWriter>()
                LogDestination.Database -> LoginLogDBWriter()
                LogDestination.Loki -> pipeline.get<LokiLogWriter>()
                else -> throw InternalServerException(
                    InfraResponseCode.SERVER_CONFIG_ERROR, "${sessionAuthConfig.logging.destination} is invalid"
                )
            }
            val logEntityDispatcher = pipeline.get<LogEntityDispatcher>()
            logEntityDispatcher.register(LoginLog.LOG_TYPE, loginLogWriter)

            val dbAsyncTaskCoroutineActor = pipeline.get<DBAsyncTaskCoroutineActor>()
            val logWriter = pipeline.get<LogWriter>()

            pipeline.koin {
                modules(
                    module(createdAtStart = true) {
                        single { LoginService(sessionStorage, dbAsyncTaskCoroutineActor, logWriter) }
                    }
                )
            }

            return feature
        }
    }
}

data class SessionAuthConfig(
    val storageType: SessionStorageType,
    val session: SessionConfig,
    val logging: LoginLogConfig,
    val redisKeyExpiredNotification: Boolean? = null
) : ValidateableConfig {

    override fun validate() {
        if (redisKeyExpiredNotification == true)
            require(storageType == SessionStorageType.Redis) {
                "storageType should be Redis if redisKeyExpiredNotification = true"
            }
    }
}