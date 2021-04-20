/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.report.utils

import fanpoll.infra.utils.LocalDateRange
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit

object ReportUtils {

    fun computePreviousRange(range: LocalDateRange, compareTimeUnit: CompareTimeUnit): LocalDateRange {
        return when (compareTimeUnit) {
            CompareTimeUnit.Month -> range.move(-1L, ChronoUnit.MONTHS) as LocalDateRange
            CompareTimeUnit.Year -> range.move(-1L, ChronoUnit.YEARS) as LocalDateRange
        }
    }

    private fun percentage(fraction: Number, denominator: Number): Double {
        val fractionDouble = if (fraction is Double) fraction else fraction.toDouble()
        val denominatorDouble = if (denominator is Double) denominator else denominator.toDouble()
        return if (denominatorDouble == 0.0) 0.00 else roundToPrecision2((fractionDouble * 100.0) / denominatorDouble)
    }

    fun percentageDiff(newValue: Number, oldValue: Number): Double =
        percentage(newValue.toDouble() - oldValue.toDouble(), oldValue)

    private fun roundToPrecision2(value: Double): Double = BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()

    val zhTWDayOfWeekStrMap: Map<DayOfWeek, String> = mapOf(
        DayOfWeek.MONDAY to "週一", DayOfWeek.TUESDAY to "週二",
        DayOfWeek.WEDNESDAY to "週三", DayOfWeek.THURSDAY to "週四", DayOfWeek.FRIDAY to "週五",
        DayOfWeek.SATURDAY to "週六", DayOfWeek.SUNDAY to "週日"
    )
}

enum class CompareTimeUnit {
    Month, Year // Year => 去年同期, Month => 上月同日
}