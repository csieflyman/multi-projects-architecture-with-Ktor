/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.club.notification

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.notification.NotificationCategory
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.channel.NotificationChannel

enum class ClubNotificationType(
    override val broadcast: Boolean,
    override val channels: Set<NotificationChannel>,
    override val category: NotificationCategory
) : NotificationType {

    ;

    override val id: String = name

    companion object {
        fun getById(id: String): ClubNotificationType = ClubNotificationType.entries.firstOrNull { it.id == id } ?: throw RequestException(
            InfraResponseCode.BAD_REQUEST_QUERYSTRING,
            "invalid ops notification type id $id"
        )
    }
}