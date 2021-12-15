/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.logging

import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.json.InstantSerializer
import fanpoll.infra.base.json.UUIDSerializer
import fanpoll.infra.database.util.ResultRowDTOMapper
import fanpoll.infra.logging.LogLevel
import fanpoll.infra.logging.LogMessage
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.channel.NotificationChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import java.time.Instant
import java.util.*

@Serializable
data class NotificationMessageLog(
    @Serializable(with = UUIDSerializer::class) override val id: UUID,
    @Serializable(with = UUIDSerializer::class) val notificationId: UUID,
    @Serializable(with = UUIDSerializer::class) val eventId: UUID,
    val type: NotificationType,
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
    var rspTime: Long? = null,
    var rspBody: String? = null
) : LogMessage() {

    @Serializable(with = InstantSerializer::class)
    override val occurAt: Instant = Instant.now()

    var content: String? = null
    var success: Boolean = true
    var errorMsg: String? = null

    override val logLevel: LogLevel = if (success) LogLevel.INFO else LogLevel.ERROR

    override val logType: String = LOG_TYPE

    companion object {
        const val LOG_TYPE = "notification_message"
    }
}

@Serializable
data class NotificationMessageLogDTO(@JvmField @Serializable(with = UUIDSerializer::class) val id: UUID) : EntityDTO<UUID> {

    @Serializable(with = UUIDSerializer::class)
    var notificationId: UUID? = null

    @Serializable(with = UUIDSerializer::class)
    var eventId: UUID? = null

    var type: String? = null
    var version: String? = null
    var channel: NotificationChannel? = null
    var lang: Lang? = null
    var receivers: String? = null

    @Serializable(with = InstantSerializer::class)
    var sendAt: Instant? = null

    // result
    var successList: JsonArray? = null
    var failureList: JsonArray? = null
    var invalidRecipientIds: String? = null

    var rspCode: String? = null
    var rspMsg: String? = null

    @Serializable(with = InstantSerializer::class)
    var rspAt: Instant? = null

    var rspTime: Long? = null
    var rspBody: String? = null

    override fun getId(): UUID = id

    companion object {
        val mapper: ResultRowDTOMapper<NotificationMessageLogDTO> =
            ResultRowDTOMapper(NotificationMessageLogDTO::class, NotificationMessageLogTable)
    }
}