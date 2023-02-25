/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.base.location

import fanpoll.infra.base.util.DateTimeUtils
import io.ktor.util.converters.DataConversion
import java.time.*
import java.util.*

object LocationUtils {

    val DataConverter: DataConversion.Configuration.() -> Unit = {
        convert<UUID> {
            decode { values ->
                UUID.fromString(values.single())
            }
            encode { value ->
                listOf(value.toString())
            }
        }
        convert<ZoneId> {
            decode { values ->
                ZoneId.of(values.single())
            }
            encode { value ->
                listOf((value as ZoneId).id)
            }
        }
        convert<Instant> {
            decode { values ->
                ZonedDateTime.parse(values.single(), DateTimeUtils.UTC_DATE_TIME_FORMATTER).toInstant()
            }
            encode { value ->
                listOf(DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(value as Instant))
            }
        }
        convert<ZonedDateTime> {
            decode { values ->
                ZonedDateTime.parse(values.single(), DateTimeUtils.UTC_DATE_TIME_FORMATTER)
            }
            encode { value ->
                listOf(DateTimeUtils.UTC_DATE_TIME_FORMATTER.format(value as ZonedDateTime))
            }
        }
        convert<LocalDateTime> {
            decode { values ->
                LocalDateTime.parse(values.single(), DateTimeUtils.LOCAL_DATE_TIME_FORMATTER)
            }
            encode { value ->
                listOf(DateTimeUtils.LOCAL_DATE_TIME_FORMATTER.format(value as LocalDateTime))
            }
        }
        convert<LocalDate> {
            decode { values ->
                LocalDate.parse(values.single(), DateTimeUtils.LOCAL_DATE_FORMATTER)
            }
            encode { value ->
                listOf(DateTimeUtils.LOCAL_DATE_FORMATTER.format(value as LocalDate))
            }
        }
        convert<LocalTime> {
            decode { values ->
                LocalTime.parse(values.single(), DateTimeUtils.LOCAL_TIME_FORMATTER)
            }
            encode { value ->
                listOf(DateTimeUtils.LOCAL_TIME_FORMATTER.format(value as LocalTime))
            }
        }
    }
}