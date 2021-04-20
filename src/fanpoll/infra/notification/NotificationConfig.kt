/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification

import fanpoll.infra.notification.channel.NotificationChannelConfig

data class NotificationConfig(
    val channels: NotificationChannelConfig
)