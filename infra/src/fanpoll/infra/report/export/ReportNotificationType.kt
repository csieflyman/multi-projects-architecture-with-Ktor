/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.export

import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.notification.NotificationCategory
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.channel.NotificationChannel


enum class ReportNotificationType(
    override val broadcast: Boolean,
    override val channels: Set<NotificationChannel>,
    override val category: NotificationCategory
) : NotificationType {

    ExportReportEmailAttachment(false, setOf(NotificationChannel.Email), NotificationCategory.System),
    DownloadReport(false, setOf(NotificationChannel.Email, NotificationChannel.SMS, NotificationChannel.Push), NotificationCategory.System);

    override val id: String = name

    companion object {
        fun getById(id: String): ReportNotificationType = entries.firstOrNull { it.id == id } ?: throw RequestException(
            InfraResponseCode.BAD_REQUEST_QUERYSTRING,
            "invalid report notification type id $id"
        )
    }
}

