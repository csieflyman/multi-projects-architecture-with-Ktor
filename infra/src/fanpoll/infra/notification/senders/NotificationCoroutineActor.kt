/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.senders

import fanpoll.infra.base.async.CoroutineActor
import fanpoll.infra.base.async.CoroutineActorConfig
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.notification.Notification
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel

class NotificationCoroutineActor(
    coroutineActorConfig: CoroutineActorConfig,
    private val notificationSender: NotificationSender
) : NotificationSender {

    private val logger = KotlinLogging.logger {}

    private val actorName = "NotificationCoroutineActor"

    private val actor: CoroutineActor<Notification> = CoroutineActor(
        actorName, Channel.UNLIMITED,
        coroutineActorConfig, Dispatchers.IO,
        this::execute, null,
    )

    override suspend fun send(notification: Notification) {
        actor.sendToUnlimitedChannel(notification, InfraResponseCode.NOTIFICATION_ERROR) // non-blocking by Channel.UNLIMITED
    }

    private suspend fun execute(notification: Notification) {
        notificationSender.send(notification)
    }

    override fun shutdown() {
        logger.info { "shutdown NotificationCoroutineActor..." }
        notificationSender.shutdown()
        actor.close()
        logger.info { "shutdown NotificationCoroutineActor completed" }
    }
}