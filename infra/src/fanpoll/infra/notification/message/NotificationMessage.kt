/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.message

import fanpoll.infra.base.util.IdentifiableObject
import fanpoll.infra.i18n.Lang
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.notification.channel.NotificationChannelContent
import fanpoll.infra.notification.logging.NotificationMessageLog
import java.time.Instant
import java.util.*

data class NotificationMessage(
    val notificationId: UUID,
    val projectId: String,
    val traceId: String? = null,
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
        id, notificationId, projectId, traceId, eventId,
        type, version, channel, lang, receivers
    )
}