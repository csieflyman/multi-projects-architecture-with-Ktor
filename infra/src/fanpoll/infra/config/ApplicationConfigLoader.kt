/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.github.oshai.kotlinlogging.KotlinLogging

// com.typesafe.config.Config is private val in ktor HoconApplicationConfig
object ApplicationConfigLoader {

    private val logger = KotlinLogging.logger {}

    private lateinit var appConfig: MyApplicationConfig

    init {
        Config4kConfigurer.registerCustomType()
    }

    fun load(): MyApplicationConfig {
        if (this::appConfig.isInitialized)
            return appConfig
        val typeSafeConfig = loadAppConfigFromFile()
        appConfig = convertTypeSafeConfigToConfigObject(typeSafeConfig)
        configureAppConfig(appConfig)
        return appConfig
    }

    private fun loadAppConfigFromFile(): Config {
        return try {
            logger.info { "load application config file..." }
            ConfigFactory.load()
            //logger.debug(myConfig.getConfig("app").entrySet().toString())
        } catch (e: Throwable) {
            logger.error(e) { "fail to load project config file" }
            throw e
        }
    }

    private fun convertTypeSafeConfigToConfigObject(config: Config): MyApplicationConfig {
        return config.extract("app")
    }

    private fun configureAppConfig(appConfig: MyApplicationConfig) {
        with(appConfig.infra.logging) {
            appInfo = appConfig.info
            server = appConfig.server
        }
        with(appConfig.infra.openApi) {
            appInfo = appConfig.info
            server = appConfig.server
        }
    }
}