/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.message

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.notification.channel.NotificationChannelSender
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

class DefaultNotificationMessageSender(
    private val channelSenders: Map<NotificationChannel, NotificationChannelSender>
) : NotificationMessageSender {

    private val logger = KotlinLogging.logger {}
    override suspend fun send(notificationMessages: List<NotificationMessage>) {
        notificationMessages.forEach { message ->
            logger.debug { "sendNotificationMessage: ${message.debugString()}" }
            val sender = channelSenders[message.channel] ?: throw InternalServerException(
                InfraResponseCode.SERVER_CONFIG_ERROR,
                "NotificationChannelSender ${message.channel} is not configured"
            )
            if (message.receivers.size <= sender.maxReceiversPerRequest) {
                message.sendAt = Instant.now()
                sender.send(message)
            } else {
                var start = 0
                while (start < message.receivers.size) {
                    val end = (start + sender.maxReceiversPerRequest).coerceAtMost(message.receivers.size)
                    val receivers = message.receivers.subList(start, end)
                    val subMessage = message.copy(receivers = receivers)
                    subMessage.sendAt = Instant.now()
                    logger.debug { "sendSubNotificationMessage: ${subMessage.debugString()}" }
                    sender.send(subMessage)
                    start += sender.maxReceiversPerRequest
                }
            }
        }
    }

    override fun shutdown() {
        logger.info { "shutdown DefaultNotificationMessageSender..." }
        channelSenders.values.forEach { it.shutdown() }
        logger.info { "shutdown DefaultNotificationMessageSender completed" }
    }
}