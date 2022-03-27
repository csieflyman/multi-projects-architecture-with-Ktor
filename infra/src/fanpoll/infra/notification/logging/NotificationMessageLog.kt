/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.logging

import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.json.DurationMicroSerializer
import fanpoll.infra.base.json.InstantSerializer
import fanpoll.infra.base.json.UUIDSerializer
import fanpoll.infra.logging.LogEntity
import fanpoll.infra.logging.LogLevel
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.channel.NotificationChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import java.time.Duration
import java.time.Instant
import java.util.*

@Serializable
data class NotificationMessageLog(
    @Serializable(with = UUIDSerializer::class) override val id: UUID,
    @Serializable(with = UUIDSerializer::class) val notificationId: UUID,
    @Serializable(with = UUIDSerializer::class) val eventId: UUID,
    val notificationType: NotificationType,
    val version: String? = null,
    val channel: NotificationChannel,
    val lang: Lang,
    val receivers: List<String>,
    @Serializable(with = InstantSerializer::class) var sendAt: Instant? = null,
    // result
    var successList: JsonArray? = null,
    var failureList: JsonArray? = null,
    var invalidRecipientIds: List<String>? = null,
    // response detail
    var rspCode: String? = null,
    var rspMsg: String? = null,
    @Serializable(with = InstantSerializer::class) var rspAt: Instant? = null,
    @Serializable(with = DurationMicroSerializer::class) var duration: Duration? = null,
    var rspBody: String? = null
) : LogEntity() {

    @Serializable(with = InstantSerializer::class)
    override val occurAt: Instant = Instant.now()

    var content: String? = null
    var success: Boolean = true
    var errorMsg: String? = null

    override val level: LogLevel = if (success) LogLevel.INFO else LogLevel.ERROR

    override val type: String = LOG_TYPE

    companion object {
        const val LOG_TYPE = "notification_message"
    }
}