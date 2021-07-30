/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.notification.channel.email

import fanpoll.infra.base.extension.myEquals
import fanpoll.infra.base.extension.myHashCode
import fanpoll.infra.base.json.json
import fanpoll.infra.notification.channel.NotificationChannelContent
import fanpoll.infra.report.data.ReportData
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement

@Serializable
class EmailContent(
    var subject: String? = null,
    var body: String? = null,
    @Transient val attachments: List<Attachment>? = null
) : NotificationChannelContent {

    override fun toJson(): JsonElement = json.encodeToJsonElement(kotlinx.serialization.serializer(), this)
}

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