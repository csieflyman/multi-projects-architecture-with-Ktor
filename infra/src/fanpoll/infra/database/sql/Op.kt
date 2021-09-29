/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.sql

import org.jetbrains.exposed.sql.*
import java.time.*

infix fun ExpressionWithColumnType<Instant>.inRange(pair: Pair<Instant, Instant>): AndOp = inTemporalRange(this, pair)

infix fun ExpressionWithColumnType<Instant>.inZonedDateTimeRange(pair: Pair<ZonedDateTime, ZonedDateTime>): AndOp =
    inTemporalRange(this, pair.first.toInstant() to pair.second.toInstant())

fun ExpressionWithColumnType<Instant>.inLocalDateTimeRange(
    start: LocalDateTime,
    endExclusive: LocalDateTime,
    zoneId: ZoneId
): AndOp = inTemporalRange(this, start.atZone(zoneId).toInstant() to endExclusive.atZone(zoneId).toInstant())

infix fun ExpressionWithColumnType<LocalDate>.inDateRange(pair: Pair<LocalDate, LocalDate>): AndOp = inTemporalRange(this, pair)

private fun <T, R> inTemporalRange(column: ExpressionWithColumnType<T>, pair: Pair<R, R>): AndOp = AndOp(
    listOf(
        GreaterEqOp(column, QueryParameter(pair.first, column.columnType)),
        LessOp(column, QueryParameter(pair.second, column.columnType))
    )
)

fun <T, R> SqlExpressionBuilder.overlap(
    startColumn: ExpressionWithColumnType<T>,
    endColumn: ExpressionWithColumnType<T>,
    rangeStart: R,
    rangeEndInclusive: R
): AndOp = AndOp(
    listOf(
        LessEqOp(startColumn, QueryParameter(rangeEndInclusive, startColumn.columnType)),
        GreaterEqOp(endColumn, QueryParameter(rangeStart, endColumn.columnType))
    )
)

fun SqlExpressionBuilder.multiIn(columns: List<Column<*>>, values: List<List<Any>>) = object : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append(columns.joinToString(",", prefix = "(", postfix = ")") {
            it.name
        })
        append(" in ")
        append(values.joinToString(",", prefix = "(", postfix = ")") { it1 ->
            it1.joinToString(",", prefix = "(", postfix = ")") { it2 ->
                when (it2) {
                    is String -> "'$it2'"
                    else -> it2.toString()
                }
            }
        })
    }
}