/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.notification

import fanpoll.infra.base.async.AsyncExecutorConfig
import fanpoll.infra.logging.LogDestination
import fanpoll.infra.notification.channel.NotificationChannelConfig

data class NotificationConfig(
    val channels: NotificationChannelConfig,
    val logging: NotificationLogConfig,
    val asyncExecutor: AsyncExecutorConfig? = null,
)

data class NotificationLogConfig(
    val enabled: Boolean = true,
    val destination: LogDestination = LogDestination.File,
    val logSuccess: Boolean = false,
    val logSuccessReqBody: Boolean = false,
    val logSuccessRspBody: Boolean = false
)