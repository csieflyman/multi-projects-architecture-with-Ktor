/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification

import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.json.kotlinx.InstantSerializer
import fanpoll.infra.base.json.kotlinx.UUIDSerializer
import fanpoll.infra.base.util.IdentifiableObject
import fanpoll.infra.i18n.Lang
import fanpoll.infra.notification.channel.NotificationChannelContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant
import java.util.*

@Serializable
open class Notification(
    val projectId: String,
    val type: NotificationType,
    val recipients: MutableSet<Recipient> = mutableSetOf(),
    val content: NotificationContent = NotificationContent(),
    val version: String = "1.0",
    val traceId: String? = null,
    @Serializable(with = UUIDSerializer::class) val eventId: UUID = UUID.randomUUID(),
    val dataLoader: NotificationDataLoader? = null
) : IdentifiableObject<UUID>() {

    @Serializable(with = UUIDSerializer::class)
    override val id: UUID = UUID.randomUUID()

    @Serializable(with = InstantSerializer::class)
    val createAt: Instant = Instant.now()

    @Serializable(with = InstantSerializer::class)
    var sendAt: Instant? = null

    @Transient
    var isLoaded = false

    fun debugString(): String =
        "$id - $eventId - [${type.id} ($version)] => $recipients"

    suspend fun loadData() {
        if (dataLoader != null && !isLoaded)
            dataLoader.load(this)
    }
}

@Serializable
class Recipient(
    override val id: String,
    val userType: UserType? = null,
    @Serializable(with = UUIDSerializer::class) val userId: UUID? = null,
    // val channels: Set<NotificationChannel>? = null, TODO => user notification preferences
    val name: String,
    var lang: Lang? = null,
    val email: String? = null,
    val mobile: String? = null,
    val pushTokens: Set<String>? = null,
    val templateArgs: MutableMap<String, String> = mutableMapOf()
) : IdentifiableObject<String>()

@Serializable
class NotificationContent(
    val channels: MutableList<NotificationChannelContent> = mutableListOf(),
    val args: MutableMap<String, String> = mutableMapOf()
)

interface NotificationDataLoader {
    suspend fun load(notification: Notification)
}