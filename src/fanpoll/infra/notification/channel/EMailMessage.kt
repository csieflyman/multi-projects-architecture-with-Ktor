/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel

import fanpoll.infra.notification.NotificationChannelMessage
import fanpoll.infra.notification.NotificationType
import fanpoll.infra.notification.Recipient
import fanpoll.infra.report.data.ReportData
import fanpoll.infra.utils.json
import fanpoll.infra.utils.myEquals
import fanpoll.infra.utils.myHashCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString

@Serializable
data class EmailMessage(
    override val type: NotificationType,
    override val recipients: Set<Recipient>? = null,
    val to: Set<String>,
    var sender: String? = null,
    override val entityType: String? = null,
    override val entityId: String? = null,
    override val content: EmailMessageContent
) : NotificationChannelMessage() {

    override val version: String? = type.version

    init {
        require(type.channels.contains(NotificationChannel.Email))
    }

    override fun toString(): String = listOf(id, type.id, to).toString()

    fun toLogDTO(): NotificationChannelLog = NotificationChannelLog(
        id, type.id, NotificationChannel.Email, to, createTime, sendTime!!, json.encodeToString(content)
    )
}

@Serializable
class EmailMessageContent(
    var subject: String,
    var body: String,
    @Transient var attachments: List<Attachment>? = null
)

class Attachment(
    fileName: String,
    val content: ByteArray,
    val mimeType: AttachmentMimeType = AttachmentMimeType.EXCEL,
) {

    var fileName: String = ""
        set(value) {
            field = "$value.${mimeType.extension}"
        }

    init {
        this.fileName = fileName
    }

    override fun equals(other: Any?) = myEquals(other, { fileName })
    override fun hashCode() = myHashCode({ fileName })
    override fun toString(): String = "$fileName($mimeType)"
}

enum class AttachmentMimeType(val value: String, val extension: String) {
    EXCEL("application/vnd.ms-excel", "xlsx"),
    CSV("text/csv", "csv"),
    PDF("application/pdf", "pdf"),
    JSON("application/json", "json")
}

fun ReportData.toExcelAttachment(fileName: String): Attachment = Attachment(fileName, toExcel(), AttachmentMimeType.EXCEL)

fun ReportData.toCsvAttachment(fileName: String): Attachment = Attachment(fileName, toCSV(), AttachmentMimeType.CSV)

fun ReportData.toJsonAttachment(fileName: String): Attachment =
    Attachment(fileName, toJson().toString().toByteArray(), AttachmentMimeType.JSON)