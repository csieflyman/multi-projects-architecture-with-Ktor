/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.push

import fanpoll.infra.base.json.json
import fanpoll.infra.notification.channel.NotificationChannelContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
class PushContent(
    val title: String,
    val body: String,
    val data: Map<String, String>? = null
) : NotificationChannelContent {

    override fun toJson(): JsonElement = json.encodeToJsonElement(kotlinx.serialization.serializer(), this)
}