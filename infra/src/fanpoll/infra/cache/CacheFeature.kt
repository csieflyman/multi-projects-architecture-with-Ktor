/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.cache

import fanpoll.infra.MyApplicationConfig
import fanpoll.infra.cache.redis.RedisCache
import fanpoll.infra.redis.ktorio.RedisClient
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey
import mu.KotlinLogging
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.koin

class CacheFeature(configuration: Configuration) {

    class Configuration {

        lateinit var caches: List<String>

        fun build(): CacheConfig {
            return CacheConfig(caches)
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, CacheFeature> {

        override val key = AttributeKey<CacheFeature>("Cache")

        private val logger = KotlinLogging.logger {}

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): CacheFeature {
            val configuration = Configuration().apply(configure)
            val feature = CacheFeature(configuration)

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
            return feature
        }
    }
}

data class CacheConfig(
    val caches: List<String>
)