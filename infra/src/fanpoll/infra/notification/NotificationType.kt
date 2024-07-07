/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification

import fanpoll.infra.notification.channel.NotificationChannel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(NotificationType.Companion::class)
interface NotificationType {
    val id: String
    val broadcast: Boolean
    val channels: Set<NotificationChannel>
    val category: NotificationCategory

    companion object : KSerializer<NotificationType> {

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.notification.NotificationType", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): NotificationType {
            error("unsupported operation")
        }

        override fun serialize(encoder: Encoder, value: NotificationType) {
            encoder.encodeString(value.id)
        }
    }
}

enum class NotificationCategory {
    System, Marketing
}