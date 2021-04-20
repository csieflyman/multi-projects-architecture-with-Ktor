/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.utils

import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeUtils {

    const val YEAR_PATTERN = "yyyy"
    const val YEAR_MONTH_PATTERN = "yyyy-MM"
    const val LOCAL_DATE_PATTERN = "yyyy-MM-dd"
    const val LOCAL_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"
    const val LOCAL_TIME_PATTERN = "HH:mm:ss"
    const val UTC_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    val UTC_ZONE_ID: ZoneId = ZoneId.of("UTC")
    val UTC_DATE_TIME_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern(UTC_DATE_TIME_PATTERN).withZone(UTC_ZONE_ID)

    val YEAR_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(YEAR_PATTERN)
    val YEAR_MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(YEAR_MONTH_PATTERN)
    val LOCAL_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(LOCAL_DATE_PATTERN)
    val LOCAL_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_PATTERN)
    val LOCAL_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(LOCAL_TIME_PATTERN)

    val TAIWAN_ZONE_ID: ZoneId = ZoneId.of("Asia/Taipei")
    val TAIWAN_DATE_TIME_FORMATTER: DateTimeFormatter = LOCAL_DATE_TIME_FORMATTER.withZone(TAIWAN_ZONE_ID)
}