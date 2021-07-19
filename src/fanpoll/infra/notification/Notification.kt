/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification

import fanpoll.infra.auth.principal.UserType
import fanpoll.infra.base.i18n.Lang
import fanpoll.infra.base.json.InstantSerializer
import fanpoll.infra.base.json.UUIDSerializer
import fanpoll.infra.base.util.IdentifiableObject
import fanpoll.infra.notification.channel.email.EmailContent
import fanpoll.infra.notification.channel.push.PushContent
import fanpoll.infra.notification.channel.sms.SMSContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.util.*

@Serializable
data class Notification(
    val type: NotificationType,
    val recipients: MutableSet<Recipient> = mutableSetOf(),
    val content: NotificationContent = NotificationContent(),
    val contentArgs: MutableMap<String, String> = mutableMapOf(),
    @Transient val templateArgs: MutableMap<String, Any> = mutableMapOf(), // templateArgs doesn't support i18n now
    @Transient val lazyLoadArg: Any? = null,
    val remote: Boolean = false,
    val remoteArg: JsonObject? = null,
    @Serializable(with = UUIDSerializer::class) override val id: UUID = UUID.randomUUID(),
    @Serializable(with = UUIDSerializer::class) val eventId: UUID = UUID.randomUUID(),
    @Serializable(with = InstantSerializer::class) val createAt: Instant = Instant.now(),
    var version: String? = null
) : IdentifiableObject<UUID>() {

    @Serializable(with = InstantSerializer::class)
    var sendAt: Instant? = null

    init {
        if (version == null)
            version = type.version
        if (version != null)
            contentArgs["version"] = version!!
    }

    fun debugString(): String =
        "$id - $eventId - [${type.id} ${version?.let { "($it)" } ?: ""}] => $recipients"

    @Transient
    val lazyLoad = type.isLazy()

    fun load() = type.lazyLoad(this)
}

@Serializable
data class Recipient(
    override val id: String,
    val userType: UserType? = null,
    @Serializable(with = UUIDSerializer::class) val userId: UUID? = null,
    // val channels: Set<NotificationChannel>? = null, TODO => user notification preferences
    val name: String? = null,
    var lang: Lang? = null,
    val email: String? = null,
    val mobile: String? = null,
    val pushTokens: Set<String>? = null
) : IdentifiableObject<String>()

@Serializable
data class NotificationContent(
    val email: MutableMap<Lang, EmailContent> = mutableMapOf(),
    val push: MutableMap<Lang, PushContent> = mutableMapOf(),
    val sms: MutableMap<Lang, SMSContent> = mutableMapOf()
)