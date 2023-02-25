/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.cache

import fanpoll.infra.MyApplicationConfig
import fanpoll.infra.cache.redis.RedisCache
import fanpoll.infra.redis.ktorio.RedisClient
import io.ktor.server.application.Application
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.util.AttributeKey
import mu.KotlinLogging
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.koin

class CachePlugin(configuration: Configuration) {

    class Configuration {

        lateinit var caches: List<String>

        fun build(): CacheConfig {
            return CacheConfig(caches)
        }
    }

    companion object Plugin : BaseApplicationPlugin<Application, Configuration, CachePlugin> {

        override val key = AttributeKey<CachePlugin>("Cache")

        private val logger = KotlinLogging.logger {}

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): CachePlugin {
            val configuration = Configuration().apply(configure)
            val plugin = CachePlugin(configuration)

            val appConfig = pipeline.get<MyApplicationConfig>()
            val cacheConfig = appConfig.infra.cache ?: configuration.build()

            if (cacheConfig.caches.isNotEmpty()) {
                val redisClient = pipeline.get<RedisClient>()
                pipeline.koin {
                    modules(
                        module(createdAtStart = true) {
                            cacheConfig.caches.forEach { cacheName ->
                                single<Cache<String, String>>(named(cacheName)) { RedisCache(redisClient, cacheName) }
                            }
                        }
                    )
                }
            }
            return plugin
        }
    }
}

data class CacheConfig(
    val caches: List<String>
)