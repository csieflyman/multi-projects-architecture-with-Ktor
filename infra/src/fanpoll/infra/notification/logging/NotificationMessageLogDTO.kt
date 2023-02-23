/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.logging

import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.json.DurationMicroSerializer
import fanpoll.infra.base.json.InstantSerializer
import fanpoll.infra.base.json.UUIDSerializer
import fanpoll.infra.database.util.ResultRowDTOMapper
import fanpoll.infra.notification.channel.NotificationChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import java.time.Duration
import java.time.Instant
import java.util.*

@Serializable
data class NotificationMessageLogDTO(
    @JvmField @Serializable(with = UUIDSerializer::class) val id: UUID
) : EntityDTO<UUID> {

    @Serializable(with = UUIDSerializer::class)
    var notificationId: UUID? = null

    var traceId: String? = null

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

    @Serializable(with = DurationMicroSerializer::class)
    var duration: Duration? = null
    var rspBody: String? = null

    override fun getId(): UUID = id

    companion object {
        val mapper: ResultRowDTOMapper<NotificationMessageLogDTO> =
            ResultRowDTOMapper(NotificationMessageLogDTO::class, NotificationMessageLogTable)
    }
}