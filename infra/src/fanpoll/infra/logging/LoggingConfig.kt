/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.logging

import fanpoll.infra.base.async.AsyncExecutorConfig
import fanpoll.infra.config.AppInfoConfig
import fanpoll.infra.config.ServerConfig
import fanpoll.infra.logging.error.ErrorLogConfig
import fanpoll.infra.logging.request.RequestLogConfig
import fanpoll.infra.logging.writers.LokiConfig
import fanpoll.infra.logging.writers.SentryConfig

data class LoggingConfig(
    val request: RequestLogConfig,
    val error: ErrorLogConfig,
    val asyncExecutor: AsyncExecutorConfig? = null,
    val writers: LoggingWritersConfig? = null
) {
    lateinit var appInfo: AppInfoConfig
    lateinit var server: ServerConfig
}

data class LoggingWritersConfig(
    val loki: LokiConfig? = null,
    val sentry: SentryConfig? = null
)