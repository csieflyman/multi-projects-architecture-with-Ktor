/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.app

import fanpoll.infra.cache.CacheConfig
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey
import mu.KotlinLogging
import org.koin.dsl.module
import org.koin.ktor.ext.koin

class AppFeature(configuration: Configuration) {

    class Configuration {

        lateinit var caches: List<String>

        fun build(): CacheConfig {
            return CacheConfig(caches)
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, AppFeature> {

        override val key = AttributeKey<AppFeature>("App")

        private val logger = KotlinLogging.logger {}

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): AppFeature {
            val configuration = Configuration().apply(configure)
            val feature = AppFeature(configuration)

            pipeline.koin {
                modules(
                    module(createdAtStart = true) {
                        single { AppReleaseService() }
                        single { PushTokenStorage(get()) }
                    }
                )
            }
            return feature
        }
    }
}