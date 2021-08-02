/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.senders

import fanpoll.infra.base.async.CoroutineActor
import fanpoll.infra.base.async.CoroutineActorConfig
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.notification.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel

class NotificationCoroutineActor(
    coroutineActorConfig: CoroutineActorConfig,
    private val notificationSender: NotificationSender,
    private val logWriter: LogWriter
) : NotificationSender {

    private val actorName = "NotificationActor"

    private val actor: CoroutineActor<Notification> = CoroutineActor(
        actorName, Channel.UNLIMITED,
        coroutineActorConfig, Dispatchers.IO,
        this::execute, null,
        logWriter
    )

    override fun send(notification: Notification) {
        actor.sendToUnlimitedChannel(notification, InfraResponseCode.NOTIFICATION_ERROR) // non-blocking by Channel.UNLIMITED
    }

    private fun execute(notification: Notification) {
        notificationSender.send(notification)
    }

    override fun shutdown() {
        notificationSender.shutdown()
        actor.close()
    }
}