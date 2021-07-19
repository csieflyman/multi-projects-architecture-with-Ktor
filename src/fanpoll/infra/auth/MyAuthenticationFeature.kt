/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.auth

import fanpoll.MyApplicationConfig
import fanpoll.infra.auth.login.LoginService
import fanpoll.infra.auth.login.logging.LoginLog
import fanpoll.infra.auth.login.logging.LoginLogDBWriter
import fanpoll.infra.auth.login.session.*
import fanpoll.infra.auth.principal.UserPrincipal
import fanpoll.infra.database.util.DBAsyncTaskCoroutineActor
import fanpoll.infra.logging.LogDestination
import fanpoll.infra.logging.writers.AwsKinesisLogWriter
import fanpoll.infra.logging.writers.FileLogWriter
import fanpoll.infra.logging.writers.LogMessageDispatcher
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.redis.RedisKeyspaceNotificationListener
import fanpoll.infra.redis.ktorio.RedisClient
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.sessions.SessionSerializer
import io.ktor.sessions.SessionStorage
import io.ktor.util.AttributeKey
import mu.KotlinLogging
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.koin

class MyAuthenticationFeature(configuration: Configuration) {

    class Configuration {

        private lateinit var logging: AuthLogConfig
        private lateinit var session: SessionConfig
        var subscribeRedisSessionKeyExpired: Boolean = true

        fun logging(configure: AuthLogConfig.Builder.() -> Unit) {
            logging = AuthLogConfig.Builder().apply(configure).build()
        }

        fun session(configure: SessionConfig.Builder.() -> Unit) {
            session = SessionConfig.Builder().apply(configure).build().apply { validate() }
        }

        fun build(): AuthConfig {
            return AuthConfig(logging, session, subscribeRedisSessionKeyExpired)
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, MyAuthenticationFeature> {

        override val key = AttributeKey<MyAuthenticationFeature>("MyAuthentication")

        private val logger = KotlinLogging.logger {}

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): MyAuthenticationFeature {
            val configuration = Configuration().apply(configure)
            val feature = MyAuthenticationFeature(configuration)

            val appConfig = pipeline.get<MyApplicationConfig>()
            val authConfig = appConfig.infra.auth ?: configuration.build()

            val loginLogWriter = when (authConfig.logging.destination) {
                LogDestination.File -> pipeline.get<FileLogWriter>()
                LogDestination.Database -> LoginLogDBWriter()
                LogDestination.AwsKinesis -> pipeline.get<AwsKinesisLogWriter>()
            }
            val logMessageDispatcher = pipeline.get<LogMessageDispatcher>()
            logMessageDispatcher.register(LoginLog.LOG_TYPE, loginLogWriter)
            val logWriter = pipeline.get<LogWriter>()

            val redisClient = pipeline.get<RedisClient>()
            val redisKeyspaceNotificationListener = if (authConfig.subscribeRedisSessionKeyExpired) {
                pipeline.get<RedisKeyspaceNotificationListener>() //NoBeanDefFoundException
            } else null
            val sessionService = RedisSessionService(authConfig.session, redisClient, redisKeyspaceNotificationListener, logWriter)
            val dbAsyncTaskCoroutineActor = pipeline.get<DBAsyncTaskCoroutineActor>()

            pipeline.koin {
                modules(
                    module(createdAtStart = true) {
                        single<SessionService> { sessionService }
                        single<SessionStorage> { DefaultSessionStorage(sessionService) }
                        single<SessionSerializer<UserPrincipal>> { DefaultSessionSerializer() }
                        single { LoginService(sessionService, dbAsyncTaskCoroutineActor, logWriter) }
                    }
                )
            }

            return feature
        }
    }
}

data class AuthConfig(
    val logging: AuthLogConfig,
    val session: SessionConfig,
    val subscribeRedisSessionKeyExpired: Boolean = true
) {

    class Builder {

        private lateinit var logging: AuthLogConfig
        private lateinit var session: SessionConfig
        var subscribeRedisSessionKeyExpired: Boolean = true

        fun logging(configure: AuthLogConfig.Builder.() -> Unit) {
            logging = AuthLogConfig.Builder().apply(configure).build()
        }

        fun build(): AuthConfig {
            return AuthConfig(logging, session, subscribeRedisSessionKeyExpired)
        }
    }
}

data class AuthLogConfig(
    val enabled: Boolean = true,
    val destination: LogDestination = LogDestination.File,
) {

    class Builder {
        var enabled: Boolean = true
        var destination: LogDestination = LogDestination.File

        fun build(): AuthLogConfig {
            return AuthLogConfig(enabled, destination)
        }
    }
}