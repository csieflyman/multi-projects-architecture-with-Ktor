/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.message

interface NotificationMessageSender {
    suspend fun send(notificationMessages: List<NotificationMessage>)
    fun shutdown() {}
}

