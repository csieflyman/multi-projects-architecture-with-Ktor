/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel

import kotlinx.serialization.json.JsonElement

interface NotificationChannelContent {

    fun toJson(): JsonElement
}