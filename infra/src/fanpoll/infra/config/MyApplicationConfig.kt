/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.config

import fanpoll.infra.auth.SessionAuthConfig
import fanpoll.infra.cache.CacheConfig
import fanpoll.infra.database.InfraDatabasesConfig
import fanpoll.infra.i18n.I18nConfig
import fanpoll.infra.logging.LoggingConfig
import fanpoll.infra.notification.NotificationConfig
import fanpoll.infra.openapi.OpenApiConfig
import fanpoll.infra.redis.RedisConfig

data class MyApplicationConfig(
    val info: AppInfoConfig,
    val server: ServerConfig,
    val infra: InfraConfig
)

data class InfraConfig(
    val logging: LoggingConfig,
    val databases: InfraDatabasesConfig,
    var redis: RedisConfig, // declare var for testcontainers replace it
    val cache: CacheConfig? = null,
    val sessionAuth: SessionAuthConfig,
    val openApi: OpenApiConfig,
    val i18n: I18nConfig,
    val notification: NotificationConfig,
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