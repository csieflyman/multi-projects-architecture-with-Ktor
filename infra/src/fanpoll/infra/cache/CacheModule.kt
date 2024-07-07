/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.cache

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.cache.redis.RedisCache
import fanpoll.infra.redis.ktorio.RedisClient
import io.ktor.server.application.Application
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.getKoin

fun Application.loadCacheModule(cacheConfig: CacheConfig?) = loadKoinModules(module(createdAtStart = true) {
    if (cacheConfig != null && cacheConfig.caches.isNotEmpty()) {
        initRedisCaches(this, cacheConfig)
    }
})

private fun Application.initRedisCaches(module: Module, cacheConfig: CacheConfig) {
    val redisClient = getKoin().getOrNull<RedisClient>()
        ?: throw InternalServerException(InfraResponseCode.SERVER_CONFIG_ERROR, "RedisClient is not configured")
    cacheConfig.caches.forEach { cacheName ->
        module.single<Cache<String, String>>(named(cacheName)) { RedisCache(redisClient, cacheName) }
    }
}