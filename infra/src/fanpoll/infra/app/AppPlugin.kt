/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.app

import fanpoll.infra.cache.CacheConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.util.AttributeKey
import org.koin.dsl.module
import org.koin.ktor.plugin.koin

class AppPlugin(configuration: Configuration) {

    class Configuration {

        lateinit var caches: List<String>

        fun build(): CacheConfig {
            return CacheConfig(caches)
        }
    }

    companion object Plugin : BaseApplicationPlugin<Application, Configuration, AppPlugin> {

        override val key = AttributeKey<AppPlugin>("App")

        private val logger = KotlinLogging.logger {}

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): AppPlugin {
            val configuration = Configuration().apply(configure)
            val plugin = AppPlugin(configuration)

            pipeline.koin {
                modules(
                    module(createdAtStart = true) {
                        single { AppReleaseService() }
                        single { PushTokenStorage(get()) }
                    }
                )
            }
            return plugin
        }
    }
}