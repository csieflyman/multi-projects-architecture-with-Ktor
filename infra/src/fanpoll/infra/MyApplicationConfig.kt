/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra

import com.typesafe.config.ConfigFactory
import fanpoll.infra.auth.SessionAuthConfig
import fanpoll.infra.base.config.Config4kExt
import fanpoll.infra.base.i18n.I18nConfig
import fanpoll.infra.cache.CacheConfig
import fanpoll.infra.database.DatabaseConfig
import fanpoll.infra.logging.LoggingConfig
import fanpoll.infra.notification.NotificationConfig
import fanpoll.infra.openapi.OpenApiConfig
import fanpoll.infra.redis.RedisConfig
import io.github.config4k.extract
import io.github.oshai.kotlinlogging.KotlinLogging

data class MyApplicationConfig(
    val info: AppInfoConfig,
    val server: ServerConfig,
    val infra: InfraConfig
)

data class AppInfoConfig(
    val project: String,
    val buildTime: String,
    val git: GitInfo
)

data class GitInfo(
    val semVer: String,
    val branch: String,
    val commitId: String,
    val abbrevCommitId: String,
    val tag: String,
    val tagName: String
)

data class ServerConfig(
    val env: EnvMode,
    val instanceId: String,
    val shutDownUrl: String
)

enum class EnvMode {
    dev, test, prod
}

data class InfraConfig(
    val logging: LoggingConfig? = null,
    val database: DatabaseConfig? = null,
    val redis: RedisConfig? = null,
    val cache: CacheConfig? = null,
    val sessionAuth: SessionAuthConfig? = null,
    val openApi: OpenApiConfig? = null,
    val i18n: I18nConfig? = null,
    val notification: NotificationConfig? = null,
)

// com.typesafe.config.Config is private val in ktor HoconApplicationConfig
object ApplicationConfigLoader {

    private val logger = KotlinLogging.logger {}

    init {
        Config4kExt.registerCustomType()
    }

    fun load(): MyApplicationConfig {
        try {
            logger.info { "load application config file..." }
            val myConfig = ConfigFactory.load()
            //logger.debug(myConfig.getConfig("app").entrySet().toString())
            return myConfig.extract("app")
        } catch (e: Throwable) {
            logger.error(e) { "fail to load project config file" }
            throw e
        }
    }
}