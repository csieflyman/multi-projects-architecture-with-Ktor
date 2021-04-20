/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel

import fanpoll.infra.notification.NotificationChannelMessage
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.Recipient
import fanpoll.infra.utils.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class SMSMessage(
    override val type: NotificationType,
    override val recipients: Set<Recipient>? = null,
    val phones: Set<String>,
    override val entityType: String? = null,
    override val entityId: String? = null,
    override val content: SMSMessageContent
) : NotificationChannelMessage() {

    override val version: String? = type.version

    init {
        require(type.channels.contains(NotificationChannel.SMS))
    }

    override fun toString(): String = listOf(id, type.id).toString()

    fun toLogDTO(): NotificationChannelLog = NotificationChannelLog(
        id, type.id, NotificationChannel.SMS, phones, createTime, sendTime!!, json.encodeToString(content)
    )
}

@Serializable
class SMSMessageContent(
    var body: String
)