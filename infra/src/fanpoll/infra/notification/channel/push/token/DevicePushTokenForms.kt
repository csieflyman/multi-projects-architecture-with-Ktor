/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.push.token

import fanpoll.infra.base.entity.EntityForm
import fanpoll.infra.base.extension.toObject
import fanpoll.infra.base.json.kotlinx.UUIDSerializer
import fanpoll.infra.release.app.domain.AppOS
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class CreateDevicePushTokenForm(
    val deviceId: String,
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    val os: AppOS,
    val pushToken: String
) : EntityForm<CreateDevicePushTokenForm, DevicePushToken, String>() {

    override fun getEntityId(): String = deviceId

    override fun toEntity(): DevicePushToken = toObject(DevicePushToken::class)
}