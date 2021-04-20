/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel

import fanpoll.infra.utils.InstantSerializer
import fanpoll.infra.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import java.time.Instant
import java.util.*

@Serializable
data class NotificationChannelLog(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val type: String,
    val channel: NotificationChannel,
    val recipients: Set<String>,
    @Serializable(with = InstantSerializer::class) val createTime: Instant,
    @Serializable(with = InstantSerializer::class) val sendTime: Instant,
    val content: String? = null,

    var successList: JsonArray? = null,
    var failureList: JsonArray? = null,
    var invalidRecipientIds: List<String>? = null,

    var rspCode: String? = null,
    var rspMsg: String? = null,
    @Serializable(with = InstantSerializer::class) var rspTime: Instant? = null,
    var rspBody: String? = null
)