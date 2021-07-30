/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification

import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.util.IdentifiableObject
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.notification.channel.NotificationChannelContent
import fanpoll.infra.notification.logging.NotificationMessageLog
import java.time.Instant
import java.util.*

data class NotificationMessage(
    val notificationId: UUID,
    val eventId: UUID,
    val type: NotificationType,
    val version: String? = null,
    val channel: NotificationChannel,
    val lang: Lang,
    val sender: String? = null,
    val receivers: List<String>,
    val content: NotificationChannelContent,
    var sendAt: Instant? = null
) : IdentifiableObject<UUID>() {

    override val id: UUID = UUID.randomUUID()

    fun debugString(): String =
        "$id - $notificationId - $eventId - [${type.id}${version?.let { "($it)" } ?: ""}] ($channel)(${lang.code}) => $receivers"

    fun toNotificationMessageLog(): NotificationMessageLog = NotificationMessageLog(
        id, notificationId, eventId,
        type, version, channel, lang, receivers
    )
}