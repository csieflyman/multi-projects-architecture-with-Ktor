/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification

import fanpoll.infra.RequestException
import fanpoll.infra.ResponseCode
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.openapi.definition.OpenApiSchemaIgnore
import fanpoll.infra.utils.IdentifiableObject
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
class NotificationType(
    val projectId: String,
    val name: String,
    val channels: Set<NotificationChannel>,
    val broadcast: Boolean,
    val purpose: NotificationPurpose,
    val sendMeToo: Boolean = false,
    val version: String? = null,
) : IdentifiableObject<String>() {

    override val id: String = "${projectId}_${name}"

    @Transient
    @OpenApiSchemaIgnore
    private var buildChannelMessageBlock: (NotificationType.(Any?) -> NotificationChannelMessage)? = null

    fun configureBuildChannelMessage(block: NotificationType.(Any?) -> NotificationChannelMessage) {
        buildChannelMessageBlock = block
    }

    fun buildChannelMessage(dto: Any?): NotificationChannelMessage {
        return buildChannelMessageBlock!!.invoke(this, dto)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializer(forClass = NotificationType::class)
    companion object : KSerializer<NotificationType> {

        private val registeredTypes = mutableMapOf<String, NotificationType>()

        fun register(types: Collection<NotificationType>) {
            types.forEach {
                require(!registeredTypes.contains(it.id)) { "NotificationType ${it.id} had been registered" }
                registeredTypes[it.id] = it
            }
        }

        fun values(): List<NotificationType> = registeredTypes.values.toList()

        private fun lookup(projectId: String, name: String): NotificationType {
            val typeId = buildTypeId(projectId, name)
            return registeredTypes[typeId]
                ?: throw RequestException(ResponseCode.REQUEST_BAD_BODY, "invalid NotificationType: $typeId")
        }

        private fun buildTypeId(projectId: String, name: String): String = "${projectId}_${name}"

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.notification.NotificationType", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): NotificationType {
            val value = decoder.decodeString()
            return lookup(value.substringBefore("_"), value.substringAfter("_"))
        }

        override fun serialize(encoder: Encoder, value: NotificationType) {
            encoder.encodeString(value.id)
        }
    }
}

enum class NotificationPurpose {
    System, Marketing
}