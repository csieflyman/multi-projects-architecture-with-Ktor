/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel

import fanpoll.infra.notification.NotificationMessage

interface NotificationChannelSender {

    fun send(message: NotificationMessage)

    val maxReceiversPerRequest: Int

    fun shutdown() {}
}