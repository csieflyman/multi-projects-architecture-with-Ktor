/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.sms

import fanpoll.infra.base.json.kotlinx.json
import fanpoll.infra.i18n.Lang
import fanpoll.infra.notification.channel.NotificationChannel
import fanpoll.infra.notification.channel.NotificationChannelContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
class SMSContent(
    override val lang: Lang,
    var body: String
) : NotificationChannelContent {

    override val channel: NotificationChannel = NotificationChannel.SMS

    override fun toJson(): JsonElement = json.encodeToJsonElement(kotlinx.serialization.serializer(), this)
}