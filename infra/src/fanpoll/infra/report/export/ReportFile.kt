/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.export

import fanpoll.infra.base.extension.myEquals
import fanpoll.infra.base.extension.myHashCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class ReportFile(
    val fileName: String,
    val mimeType: MimeType
) {
    @Transient
    lateinit var content: ByteArray

    var destination: ReportFileDestination? = null
    var downloadUrl: String? = null

    val fullName = "$fileName.${mimeType.extension}"
    override fun equals(other: Any?) = myEquals(other, { fullName })
    override fun hashCode() = myHashCode({ fullName })
    override fun toString(): String = fullName

    enum class MimeType(val value: String, val extension: String) {
        CSV("text/csv", "csv"),
        EXCEL("application/vnd.ms-excel", "xlsx"),
        JSON("application/json", "JSON")
    }
}

enum class ReportFileDestination {
    GDRIVE_SERVER
}