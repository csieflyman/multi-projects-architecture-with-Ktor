/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.push.token

import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.json.kotlinx.InstantSerializer
import fanpoll.infra.base.json.kotlinx.UUIDSerializer
import fanpoll.infra.release.app.domain.AppOS
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

@Serializable
data class DevicePushTokenDTO(val deviceId: String) : EntityDTO<String> {

    @Serializable(with = UUIDSerializer::class)
    var userId: UUID? = null
    var os: AppOS? = null
    var pushToken: String? = null

    @Serializable(with = InstantSerializer::class)
    var createdAt: Instant? = null

    @Serializable(with = InstantSerializer::class)
    var updatedAt: Instant? = null

    override fun getId(): String = deviceId
}