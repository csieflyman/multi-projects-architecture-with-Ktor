/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.email

import fanpoll.infra.base.extension.myEquals
import fanpoll.infra.base.extension.myHashCode
import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.i18n.Lang
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.notification.channel.NotificationChannelContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement

@Serializable
class EmailContent(
    override val lang: Lang,
    var subject: String? = null,
    var body: String? = null,
    val attachments: List<Attachment>? = null
) : NotificationChannelContent {

    override val channel: NotificationChannel = NotificationChannel.Email

    @Serializable
    class Attachment(
        val fileName: String,
        val mimeType: String,
        @Transient val content: ByteArray = byteArrayOf()
    ) {
        override fun equals(other: Any?) = myEquals(other, { fileName })
        override fun hashCode() = myHashCode({ fileName })
        override fun toString(): String = "$fileName($mimeType)"
    }

    override fun toJson(): JsonElement = json.encodeToJsonElement(kotlinx.serialization.serializer(), this)
}