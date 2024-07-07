/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.session

import fanpoll.infra.auth.SessionAuthConfig
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.redis.RedisKeyspaceNotificationListener
import fanpoll.infra.redis.ktorio.RedisClient
import io.ktor.server.application.Application
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin

fun Application.loadSessionModule(sessionAuthConfig: SessionAuthConfig) = loadKoinModules(module(createdAtStart = true) {
    initSessionStorage(this, sessionAuthConfig)
})

private fun Application.initSessionStorage(module: Module, sessionAuthConfig: SessionAuthConfig) {
    val sessionStorage = when (sessionAuthConfig.storageType) {
        SessionStorageType.Redis -> {
            val redisClient = getKoin().getOrNull<RedisClient>()
                ?: throw InternalServerException(InfraResponseCode.SERVER_CONFIG_ERROR, "RedisClient is not configured")
            val redisKeyspaceNotificationListener = if (sessionAuthConfig.redisKeyExpiredNotification == true) {
                get<RedisKeyspaceNotificationListener>()
            } else null
            val logWriter = get<LogWriter>()
            RedisSessionStorage(redisClient, redisKeyspaceNotificationListener, logWriter)
        }
    }
    module.single<MySessionStorage> { sessionStorage }
}