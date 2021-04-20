/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel

import fanpoll.infra.EnvMode
import fanpoll.infra.notification.NotificationChannelMessage
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.Recipient
import fanpoll.infra.utils.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class PushMessage(
    override val type: NotificationType,
    override val recipients: Set<Recipient>? = null,
    val tokens: Set<String>,
    override val entityType: String? = null,
    override val entityId: String? = null,
    override val content: PushMessageContent
) : NotificationChannelMessage() {

    override val version: String? = type.version

    init {
        require(type.channels.contains(NotificationChannel.Push))
    }

    override fun toString(): String = listOf(id, type.id).toString()

    fun toLogDTO(): NotificationChannelLog = NotificationChannelLog(
        id, type.id, NotificationChannel.Push, tokens, createTime, sendTime!!, json.encodeToString(content)
    )

    fun toFCMDataMap(envMode: EnvMode): Map<String, String> {
        val title = if (envMode != EnvMode.prod) "[${envMode.name}] " + content.title
        else content.title

        val map = mutableMapOf(
            "id" to id.toString(),
            "type" to type.name,
            "broadcast" to type.broadcast.toString(),
            "purpose" to type.purpose.name,
            "title" to title,
            "body" to content.body
        )
        entityType?.also { map["entityType"] = it }
        entityId?.also { map["entityId"] = it }
        content.data?.forEach { (k, v) -> map[k] = v }
        return map.toMap()
    }
}

@Serializable
class PushMessageContent(
    var title: String,
    var body: String,
    val data: Map<String, String>? = null
)