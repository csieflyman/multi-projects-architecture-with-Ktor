/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.utils

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMapError
import com.github.kittinunf.result.getOrElse
import fanpoll.infra.RequestException
import fanpoll.infra.ResponseCode
import fanpoll.infra.utils.DateTimeUtils.LOCAL_DATE_FORMATTER
import fanpoll.infra.utils.DateTimeUtils.LOCAL_DATE_TIME_FORMATTER
import java.time.*
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.reflect.full.primaryConstructor

abstract class TemporalRange<T>(
    override val start: T,
    override val endInclusive: T,
    val rangeUnit: ChronoUnit,
    private val step: Long = 1,
    private val stepUnit: ChronoUnit
) : ClosedRange<T>, Iterable<T> where T : Temporal, T : Comparable<T> {

    abstract fun step(step: Long, stepUnit: ChronoUnit?): TemporalRange<T>

    fun move(amount: Long, unit: ChronoUnit): TemporalRange<T> {
        val constructor = javaClass.kotlin.primaryConstructor!!
        val startParameter = constructor.parameters.first { it.name == "start" }
        val endInclusiveParameter = constructor.parameters.first { it.name == "endInclusive" }
        return constructor.callBy(
            mapOf(
                startParameter to start.plus(amount, unit),
                endInclusiveParameter to endInclusive.plus(amount, unit)
            )
        )
    }

    fun isBefore(other: TemporalRange<T>): Boolean =
        other.start >= this.start && other.endInclusive >= this.endInclusive

    fun include(other: TemporalRange<T>): Boolean = other.start >= this.start && other.endInclusive <= this.endInclusive

    fun overlap(other: TemporalRange<T>): Boolean = other.start <= this.endInclusive && other.endInclusive >= this.start

    override fun contains(value: T): Boolean {
        return value in this
    }

    override fun isEmpty(): Boolean {
        return start.compareTo(endInclusive) == 0
    }

    override fun equals(other: Any?) = myEquals(other, { start }, { endInclusive })
    override fun hashCode() = myHashCode({ start }, { endInclusive })

    override fun iterator(): Iterator<T> {

        return object : Iterator<T> {

            var current: T = start

            override fun hasNext(): Boolean {
                return current <= endInclusive
            }

            override fun next(): T {
                val next = current
                current = current.plus(step, stepUnit) as T
                return next
            }
        }
    }
}

operator fun LocalDateTime.rangeTo(endInclusive: LocalDateTime) = LocalDateTimeRange(this, endInclusive)

class LocalDateTimeRange(
    start: LocalDateTime,
    endInclusive: LocalDateTime,
    rangeUnit: ChronoUnit = ChronoUnit.HOURS,
    step: Long = 1,
    stepUnit: ChronoUnit = ChronoUnit.HOURS
) : TemporalRange<LocalDateTime>(start, endInclusive, rangeUnit, step, stepUnit) {

    init {
        if (start > endInclusive)
            throw throw RequestException(ResponseCode.REQUEST_BAD_QUERY, "startTime should be before endTime")
    }

    infix fun step(step: Long) = step(step, ChronoUnit.HOURS)

    override fun step(step: Long, stepUnit: ChronoUnit?): LocalDateTimeRange {
        if (stepUnit != null)
            require(stepUnit == ChronoUnit.HOURS)
        return LocalDateTimeRange(start, endInclusive, rangeUnit, step, stepUnit ?: ChronoUnit.HOURS)
    }

    override fun toString(): String {
        return "${LOCAL_DATE_TIME_FORMATTER.format(start)} ~ ${LOCAL_DATE_TIME_FORMATTER.format(endInclusive)}"
    }

    companion object {

        fun today(zoneId: ZoneId): LocalDateTimeRange {
            val start = LocalDate.now(zoneId).atStartOfDay()
            val end = start.withHour(23).withMinute(59).withSecond(59) // second precision
            return LocalDateTimeRange(start, end, ChronoUnit.HOURS)
        }

        fun thisHour(time: LocalDateTime): LocalDateTimeRange {
            return LocalDateTimeRange(
                time.truncatedTo(ChronoUnit.MINUTES),
                time.withMinute(59).withSecond(59), // second precision
                ChronoUnit.HOURS
            )
        }

        fun parse(text: String): LocalDateTimeRange = try {
            thisHour(LocalDateTime.parse(text, LOCAL_DATE_TIME_FORMATTER))
        } catch (e: DateTimeParseException) {
            throw RequestException(ResponseCode.REQUEST_BAD_QUERY, "invalid datetime format: $text")
        }

        fun parse(startTimeText: String, endTimeText: String): LocalDateTimeRange {
            val startTime = try {
                LocalDateTime.parse(startTimeText, LOCAL_DATE_TIME_FORMATTER)
            } catch (e: DateTimeParseException) {
                throw RequestException(ResponseCode.REQUEST_BAD_QUERY, "invalid startTime format: $startTimeText")
            }
            val endTime = try {
                LocalDateTime.parse(endTimeText, LOCAL_DATE_TIME_FORMATTER)
            } catch (e: DateTimeParseException) {
                throw RequestException(ResponseCode.REQUEST_BAD_QUERY, "invalid endTime format: $endTimeText")
            }
            return LocalDateTimeRange(startTime, endTime)
        }
    }
}

operator fun LocalDate.rangeTo(endInclusive: LocalDate) = LocalDateRange(this, endInclusive)

class LocalDateRange(
    start: LocalDate,
    endInclusive: LocalDate,
    rangeUnit: ChronoUnit = ChronoUnit.DAYS,
    step: Long = 1,
    stepUnit: ChronoUnit = ChronoUnit.DAYS
) : TemporalRange<LocalDate>(start, endInclusive, rangeUnit, step, stepUnit) {

    init {
        if (start > endInclusive)
            throw throw RequestException(ResponseCode.REQUEST_BAD_QUERY, "startDate should be before endDate")
    }

    infix fun step(step: Long) = step(step, ChronoUnit.DAYS)

    override fun step(step: Long, stepUnit: ChronoUnit?): LocalDateRange {
        if (stepUnit != null)
            require(stepUnit == ChronoUnit.DAYS || stepUnit == ChronoUnit.HOURS)
        return LocalDateRange(start, endInclusive, rangeUnit, step, stepUnit ?: ChronoUnit.DAYS)
    }

    fun getDays(): Long = ChronoUnit.DAYS.between(start, endInclusive).coerceAtLeast(1)

    fun toHalfOpenRange(): Pair<LocalDate, LocalDate> {
        return start to endInclusive.plusDays(1)
    }

    fun toHalfOpenZonedDateTimeRange(zoneId: ZoneId): Pair<ZonedDateTime, ZonedDateTime> {
        return start.atStartOfDay(zoneId) to endInclusive.atStartOfDay(zoneId).plusDays(1)
    }

    fun toHalfOpenInstantRange(zoneId: ZoneId): Pair<Instant, Instant> {
        return start.atStartOfDay(zoneId).toInstant() to endInclusive.atStartOfDay(zoneId).plusDays(1).toInstant()
    }

    override fun toString(): String {
        return "${LOCAL_DATE_FORMATTER.format(start)} ~ ${LOCAL_DATE_FORMATTER.format(endInclusive)}"
    }

    companion object {

        fun today(zoneId: ZoneId): LocalDateRange {
            return thisDate(LocalDate.now(zoneId))
        }

        fun thisDate(date: LocalDate): LocalDateRange {
            return LocalDateRange(date, date, ChronoUnit.DAYS)
        }

        fun thisMonth(month: YearMonth): LocalDateRange {
            return LocalDateRange(month.atDay(1), month.atEndOfMonth(), ChronoUnit.MONTHS)
        }

        fun thisYear(year: Year): LocalDateRange {
            return LocalDateRange(year.atDay(1), year.atMonthDay(MonthDay.of(12, 31)), ChronoUnit.YEARS)
        }

        fun of(start: LocalDate, end: LocalDate): LocalDateRange {
            return LocalDateRange(start, end, ChronoUnit.DAYS)
        }

        fun of(start: YearMonth, end: YearMonth): LocalDateRange {
            return LocalDateRange(start.atDay(1), end.atEndOfMonth(), ChronoUnit.MONTHS)
        }

        fun of(start: Year, end: Year): LocalDateRange {
            return LocalDateRange(start.atDay(1), end.atMonthDay(MonthDay.of(12, 31)), ChronoUnit.YEARS)
        }

        fun of(start: Temporal, end: Temporal): LocalDateRange {
            return when {
                start is LocalDate && end is LocalDate -> of(start, end)
                start is YearMonth && end is YearMonth -> of(start, end)
                start is Year && end is Year -> of(start, end)
                else -> throw RequestException(
                    ResponseCode.REQUEST_BAD_QUERY,
                    "invalid temporal type: start = ${start.javaClass.kotlin.qualifiedName}, " +
                            "end = ${end.javaClass.kotlin.qualifiedName}"
                )
            }
        }

        fun parse(startText: String, endText: String): LocalDateRange {
            return if (startText == endText) {
                when (val startTemporal = parseToTemporal(startText)) {
                    is LocalDate -> thisDate(startTemporal)
                    is YearMonth -> thisMonth(startTemporal)
                    is Year -> thisYear(startTemporal)
                    else -> throw RequestException(
                        ResponseCode.REQUEST_BAD_QUERY,
                        "invalid temporal type: ${startTemporal.javaClass.kotlin.qualifiedName}"
                    )
                }
            } else of(parseToTemporal(startText), parseToTemporal(endText))
        }

        private fun parseToTemporal(text: String): Temporal {
            return Result.of<LocalDate, DateTimeParseException> {
                LocalDate.parse(text, LOCAL_DATE_FORMATTER)
            }.flatMapError {
                Result.of { YearMonth.parse(text, DateTimeUtils.YEAR_MONTH_FORMATTER) }
            }.flatMapError {
                Result.of { Year.parse(text, DateTimeUtils.YEAR_FORMATTER) }
            }.getOrElse { throw RequestException(ResponseCode.REQUEST_BAD_QUERY, "invalid temporal format: $text") }
        }
    }
}