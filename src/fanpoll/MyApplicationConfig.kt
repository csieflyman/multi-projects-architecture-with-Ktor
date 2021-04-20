/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import fanpoll.club.ClubConfig
import fanpoll.infra.ServerConfig
import fanpoll.infra.database.DatabaseConfig
import fanpoll.infra.logging.LoggingConfig
import fanpoll.infra.notification.NotificationConfig
import fanpoll.infra.openapi.OpenApiConfig
import fanpoll.infra.redis.RedisConfig
import fanpoll.infra.utils.myExtract
import fanpoll.ops.OpsConfig
import mu.KotlinLogging

data class MyApplicationConfig(
    val server: ServerConfig,
    val logging: LoggingConfig,
    val database: DatabaseConfig,
    val redis: RedisConfig,
    val notification: NotificationConfig,
    val openApi: OpenApiConfig,
    val ops: OpsConfig,
    val club: ClubConfig
) {

    companion object {

        private val logger = KotlinLogging.logger {}

        fun loadHOCONFile(): MyApplicationConfig {
            try {
                val appConfig: Config = ConfigFactory.load()
                //logger.debug(appConfig.getConfig("ktor.myapp").entrySet().toString())
                return appConfig.myExtract("ktor.myapp")
            } catch (e: Throwable) {
                logger.error("fail to load application config file", e)
                throw e
            }
        }
    }
}