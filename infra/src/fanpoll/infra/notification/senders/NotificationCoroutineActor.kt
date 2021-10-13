/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.senders

import fanpoll.infra.base.async.CoroutineActor
import fanpoll.infra.base.async.CoroutineActorConfig
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.notification.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging

class NotificationCoroutineActor(
    coroutineActorConfig: CoroutineActorConfig,
    private val notificationSender: NotificationSender,
    private val logWriter: LogWriter
) : NotificationSender {

    private val logger = KotlinLogging.logger {}

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
        try {
            notificationSender.send(notification)
        } catch (e: Throwable) {
            val errorMsg = "$actorName execute error"
            logger.error("errorMsg => $notification", e)
            logWriter.write(
                ErrorLog.internal(
                    InternalServerException(
                        InfraResponseCode.NOTIFICATION_ERROR, errorMsg, e,
                        mapOf("id" to notification.id, "type" to notification.type, "eventId" to notification.eventId)
                    ),
                    actorName, notification.id.toString()
                )
            )
        }
    }

    override fun shutdown() {
        notificationSender.shutdown()
        actor.close()
    }
}