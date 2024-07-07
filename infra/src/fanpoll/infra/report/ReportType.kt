/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report

import fanpoll.infra.report.export.ReportFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(ReportType.Companion::class)
interface ReportType {
    val id: String
    fun isSupportedFileFormat(mimeType: ReportFile.MimeType): Boolean

    companion object : KSerializer<ReportType> {

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.report.ReportType", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): ReportType {
            error("unsupported operation")
        }

        override fun serialize(encoder: Encoder, value: ReportType) {
            encoder.encodeString(value.id)
        }
    }
}