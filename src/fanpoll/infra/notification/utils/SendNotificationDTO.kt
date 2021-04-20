/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.utils

import fanpoll.infra.auth.UserType
import fanpoll.infra.controller.Form
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.Recipient
import fanpoll.infra.notification.channel.MultiChannelsMessageContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SendNotificationDTO(
    val type: NotificationType,
    val recipients: Set<Recipient>? = null,
    val userFilters: Map<UserType, String>? = null, // null only if type is broadcast
    val entityType: String? = null,
    val entityId: String? = null,
    val args: JsonObject? = null,
    val content: MultiChannelsMessageContent? = null
) : Form<SendNotificationDTO>()