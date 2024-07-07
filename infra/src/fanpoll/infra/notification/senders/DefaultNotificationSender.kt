/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.senders

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.i18n.AvailableLangs
import fanpoll.infra.logging.error.ErrorLog
import fanpoll.infra.logging.writers.LogWriter
import fanpoll.infra.notification.Notification
import fanpoll.infra.notification.message.NotificationMessageBuilder
import fanpoll.infra.notification.message.NotificationMessageSender
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

class DefaultNotificationSender(
    private val availableLangs: AvailableLangs,
    private val notificationMessageBuilder: NotificationMessageBuilder,
    private val notificationMessageSender: NotificationMessageSender,
    private val logWriter: LogWriter
) : NotificationSender {

    private val logger = KotlinLogging.logger {}

    override suspend fun send(notification: Notification) {
        try {
            logger.debug { "sendNotification: ${notification.debugString()}" }
            notification.loadData()
            checksAndSetDefaultValueOfRecipients(notification)
            if (notification.recipients.isEmpty()) {
                logger.warn { "notification recipients is empty! => $notification" }
                return
            }
            val notificationMessages = notificationMessageBuilder.build(notification)
            if (notification.sendAt == null)
                notification.sendAt = Instant.now()
            notificationMessageSender.send(notificationMessages)
        } catch (e: Throwable) {
            writeErrorLog(notification, e)
        }
    }

    private fun checksAndSetDefaultValueOfRecipients(notification: Notification) {
        notification.recipients.onEach {
            if (it.lang == null || !availableLangs.langs.contains(it.lang))
                it.lang = availableLangs.first()

            it.templateArgs["account"] = it.id
            it.templateArgs["name"] = it.name
        }
    }

    private suspend fun writeErrorLog(notification: Notification, e: Throwable) {
        val errorMsg = "send notification error"
        logger.error(e) { "errorMsg => $notification" }
        logWriter.write(
            ErrorLog.internal(
                InternalServerException(
                    InfraResponseCode.NOTIFICATION_ERROR, errorMsg, e,
                    mapOf(
                        "notificationId" to notification.id, "notificationType" to notification.type,
                        "eventId" to notification.eventId
                    )
                ),
                "sendNotification", mapOf("notificationId" to notification.id.toString())
            )
        )
    }

    override fun shutdown() {
        logger.info { "shutdown DefaultNotificationSender..." }
        notificationMessageSender.shutdown()
        logger.info { "shutdown DefaultNotificationSender completed" }
    }
}