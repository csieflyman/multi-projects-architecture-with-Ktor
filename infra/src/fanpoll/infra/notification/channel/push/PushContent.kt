/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.push

import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.i18n.Lang
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.notification.channel.NotificationChannelContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
class PushContent(
    override val lang: Lang,
    var title: String,
    var body: String,
    val data: MutableMap<String, String> = mutableMapOf()
) : NotificationChannelContent {

    override val channel: NotificationChannel = NotificationChannel.Push

    override fun toJson(): JsonElement = json.encodeToJsonElement(kotlinx.serialization.serializer(), this)
}