/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.senders

import fanpoll.infra.notification.Notification

interface NotificationSender {

    fun send(notification: Notification)

    fun shutdown() {}
}