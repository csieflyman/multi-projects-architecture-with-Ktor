/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel

import fanpoll.infra.i18n.Lang
import kotlinx.serialization.json.JsonElement

interface NotificationChannelContent {

    val channel: NotificationChannel
    val lang: Lang

    fun toJson(): JsonElement
}