/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.location

import fanpoll.infra.base.util.DateTimeUtils
import io.ktor.features.DataConversion
import java.time.*
import java.util.*

object LocationUtils {

    val DataConverter: DataConversion.Configuration.() -> Unit = {
        convert<UUID> {
            decode { values, _ ->
                values.singleOrNull()?.let { UUID.fromString(it) }
            }
            encode { value ->
                listOf(value.toString())
            }
        }
        convert<ZoneId> {
            decode { values, _ ->
                values.singleOrNull()?.let { ZoneId.of(it) }
            }
            encode { value ->
                listOf((value as ZoneId).id)
            }
        }
        convert<Instant> {
            decode { values, _ ->
                values.singleOrNull()?.let { ZonedDateTime.parse(it, DateTimeUtils.UTC_DATE_TIME_FORMATTER).toInstant() }
            }
            encode { value ->
                listOf(DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(value as Instant))
            }
        }
        convert<ZonedDateTime> {
            decode { values, _ ->
                values.singleOrNull()?.let { ZonedDateTime.parse(it, DateTimeUtils.UTC_DATE_TIME_FORMATTER) }
            }
            encode { value ->
                listOf(DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(value as ZonedDateTime))
            }
        }
        convert<LocalDateTime> {
            decode { values, _ ->
                values.singleOrNull()?.let { LocalDateTime.parse(it, DateTimeUtils.LOCAL_DATE_TIME_FORMATTER) }
            }
            encode { value ->
                listOf(DateTimeUtils.LOCAL_DATE_TIME_FORMATTER.format(value as LocalDateTime))
            }
        }
        convert<LocalDate> {
            decode { values, _ ->
                values.singleOrNull()?.let { LocalDate.parse(it, DateTimeUtils.LOCAL_DATE_FORMATTER) }
            }
            encode { value ->
                listOf(DateTimeUtils.LOCAL_DATE_FORMATTER.format(value as LocalDate))
            }
        }
        convert<LocalTime> {
            decode { values, _ ->
                values.singleOrNull()?.let { LocalTime.parse(it, DateTimeUtils.LOCAL_TIME_FORMATTER) }
            }
            encode { value ->
                listOf(DateTimeUtils.LOCAL_TIME_FORMATTER.format(value as LocalTime))
            }
        }
    }
}