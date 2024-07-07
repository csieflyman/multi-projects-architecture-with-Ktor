/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.report.data.table

import fanpoll.infra.base.datetime.DateTimeUtils
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

@Serializable(DataCell.Companion::class)
class DataCell(val value: Any?) {
    fun stringValue(): String = when (value) {
        null -> "null"
        is String, is UUID, is Boolean, is BigDecimal, is Number -> value.toString()
        is Date -> DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER.format(value.toInstant())
        is Instant -> DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER.format(value)
        is ZonedDateTime -> DateTimeUtils.TAIWAN_DATE_TIME_FORMATTER.format(value)
        is LocalDate -> DateTimeUtils.LOCAL_DATE_FORMATTER.format(value)
        is LocalDateTime -> DateTimeUtils.LOCAL_DATE_TIME_FORMATTER.format(value)
        else -> value.toString()
    }

    companion object : KSerializer<DataCell> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("fanpoll.infra.report.data.table.DataCell", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): DataCell {
            throw InternalServerException(InfraResponseCode.DEV_ERROR, "unsupported operation")
        }

        @OptIn(ExperimentalSerializationApi::class)
        override fun serialize(encoder: Encoder, value: DataCell) {
            if (value.value != null)
                encoder.encodeString(value.stringValue())
            else
                encoder.encodeNull()
        }
    }
}