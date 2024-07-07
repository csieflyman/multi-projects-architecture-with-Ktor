/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.export

import fanpoll.infra.base.datetime.DateTimeUtils
import fanpoll.infra.notification.Notification
import fanpoll.infra.notification.Recipient
import fanpoll.infra.notification.channel.email.EmailContent
import fanpoll.infra.notification.senders.NotificationSender
import fanpoll.infra.report.Report
import org.koin.core.component.KoinComponent
import java.time.Instant

class ReportNotificationSender : KoinComponent {
    suspend fun send(report: Report, recipients: Set<Recipient>, templateArgs: Map<String, String>? = null) {
        if (recipients.isEmpty()) return

        val notification = if (report.file.downloadUrl == null) {
            Notification(report.projectId, ReportNotificationType.ExportReportEmailAttachment).apply {
                content.channels.add(EmailContent(report.lang, attachments = listOf(
                    with(report.file) {
                        EmailContent.Attachment(fullName, mimeType.value, content)
                    }
                )))
            }
        } else
            Notification(report.projectId, ReportNotificationType.DownloadReport)

        notification.recipients.addAll(recipients)

        with(notification.content.args) {
            val reportArgs = mutableMapOf(
                "reportTitle" to report.title,
                "reportFileName" to report.file.fullName,
                "exportTime" to DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER.format(Instant.now())
            )
            if (report.file.downloadUrl != null) {
                reportArgs["reportFileDownloadUrl"] = report.file.downloadUrl!!
            }
            putAll(reportArgs)
            templateArgs?.let { putAll(it) }
        }

        val notificationSender = getKoin().get<NotificationSender>()
        notificationSender.send(notification)
    }
}