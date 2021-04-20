/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel

import fanpoll.infra.notification.NotificationChannelMessage
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.Recipient
import kotlinx.serialization.Serializable

@Serializable
data class MultiChannelsMessage(
    override val type: NotificationType,
    override val recipients: Set<Recipient>,
    override val entityType: String? = null,
    override val entityId: String? = null,
    override val content: MultiChannelsMessageContent
) : NotificationChannelMessage() {

    override val version: String? = type.version

    val channels: Set<NotificationChannel> = type.channels

    override fun toString(): String = listOf(id, type.id, channels).toString()

    fun toEmailMessage(): EmailMessage = EmailMessage(
        type, recipients, recipients.map { it.email!! }.toSet(), null, entityType, entityId, content = content.email!!
    )

    fun toPushMessage(): PushMessage = PushMessage(
        type, recipients, recipients.flatMap { it.pushTokens!! }.toSet(), entityType, entityId, content = content.push!!
    )

    fun toSMSMessage(): SMSMessage = SMSMessage(
        type, recipients, recipients.map { it.phone!! }.toSet(), entityType, entityId, content = content.sms!!
    )
}

@Serializable
class MultiChannelsMessageContent(
    val email: EmailMessageContent? = null,
    val push: PushMessageContent? = null,
    val sms: SMSMessageContent? = null
)