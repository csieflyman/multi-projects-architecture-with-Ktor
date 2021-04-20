/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification

import fanpoll.infra.auth.UserType
import fanpoll.infra.notification.channel.MultiChannelsMessageContent
import fanpoll.infra.notification.utils.SendNotificationDTO
import fanpoll.infra.utils.IdentifiableObject
import fanpoll.infra.utils.InstantSerializer
import fanpoll.infra.utils.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.util.*

typealias NotificationChannelMessage = Notification.ChannelMessage
typealias NotificationCmdMessage = Notification.CommandMessage
typealias NotificationRemoteCmdMessage = Notification.RemoteCommandMessage

@Serializable
sealed class Notification : IdentifiableObject<UUID>() {

    abstract val type: NotificationType
    abstract val recipients: Set<Recipient>?
    abstract val entityType: String?
    abstract val entityId: String?
    abstract val version: String?

    @Serializable(with = UUIDSerializer::class)
    override val id: UUID = UUID.randomUUID()

    @Serializable(with = InstantSerializer::class)
    val createTime: Instant = Instant.now()

    @Serializable(with = InstantSerializer::class)
    var sendTime: Instant? = null

    abstract class ChannelMessage : Notification() {

        var args: Any? = null

        abstract val content: Any
    }

    class CommandMessage(
        override val type: NotificationType,
        override val recipients: Set<Recipient>? = null,
        override val entityType: String? = null,
        override val entityId: String? = null,
        var args: Any? = null,
        val dto: Any? = null
    ) : Notification() {

        override val version: String? = type.version

        companion object {

            fun create(dto: SendNotificationDTO): NotificationCmdMessage =
                CommandMessage(dto.type, dto.recipients, dto.entityType, dto.entityId, args = dto.args, dto = dto)

            fun create(type: NotificationType, dto: Any): NotificationCmdMessage =
                CommandMessage(type, dto = dto)
        }

        fun buildChannelMessages(): ChannelMessage = type.buildChannelMessage(dto)
    }

    @Serializable
    class RemoteCommandMessage(
        override val type: NotificationType,
        override val recipients: Set<Recipient>? = null,
        override val entityType: String? = null,
        override val entityId: String? = null,
        val args: JsonObject? = null,
        val content: MultiChannelsMessageContent? = null
    ) : Notification() {

        override val version: String? = type.version

        fun buildChannelMessages(): ChannelMessage = type.buildChannelMessage(this)
    }
}

@Serializable
data class Recipient(
    val userType: UserType,
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    val name: String? = null,
    var email: String? = null,
    var phone: String? = null,
    var pushTokens: Set<String>? = null
) : IdentifiableObject<String>() {

    override val id: String = userId.toString()
}