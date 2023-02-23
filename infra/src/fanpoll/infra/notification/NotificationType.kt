/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification

import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.json.json
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.util.IdentifiableObject
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.openapi.schema.operation.support.OpenApiIgnore
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus

@Serializable
open class NotificationType(
    val projectId: String,
    val name: String,
    val channels: Set<NotificationChannel>,
    val category: NotificationCategory,
    // val priority: NotificationPriority TODO => priority queues
    val version: String? = null,
    val lang: Lang? = null,
    @Transient @OpenApiIgnore private val lazyLoadBlock: (NotificationType.(Notification) -> Unit)? = null
) : IdentifiableObject<String>() {

    override val id: String = "${projectId}_${name}"

    fun isLazy(): Boolean = lazyLoadBlock != null

    fun lazyLoad(notification: Notification) {
        requireNotNull(lazyLoadBlock)
        lazyLoadBlock.invoke(this, notification)
    }

    open fun findRecipients(userFilters: Map<UserType, String>?): Set<Recipient> =
        error("NotificationType $id findRecipients is not yet implemented")

    @OptIn(ExperimentalSerializationApi::class)
    @Serializer(forClass = NotificationType::class)
    companion object : KSerializer<NotificationType> {

        init {
            json.serializersModule.plus(SerializersModule {
                polymorphicDefault(NotificationType::class) { serializer() }
            })
        }

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
                ?: throw RequestException(InfraResponseCode.BAD_REQUEST_BODY_FIELD, "invalid NotificationType: $typeId")
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

enum class NotificationCategory {
    System, Marketing
}

enum class NotificationPriority {
    URGENT, HIGH, LOW
}