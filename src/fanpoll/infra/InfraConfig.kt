/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra

import fanpoll.infra.auth.AuthConfig
import fanpoll.infra.base.i18n.I18nConfig
import fanpoll.infra.cache.CacheConfig
import fanpoll.infra.database.DatabaseConfig
import fanpoll.infra.logging.LoggingConfig
import fanpoll.infra.notification.NotificationConfig
import fanpoll.infra.openapi.OpenApiConfig
import fanpoll.infra.redis.RedisConfig

data class InfraConfig(
    val i18n: I18nConfig? = null,
    val logging: LoggingConfig? = null,
    val auth: AuthConfig? = null,
    val openApi: OpenApiConfig? = null,
    val database: DatabaseConfig? = null,
    val redis: RedisConfig? = null,
    val cache: CacheConfig? = null,
    val notification: NotificationConfig? = null,
)