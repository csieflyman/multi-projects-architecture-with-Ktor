/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.sql

import org.jetbrains.exposed.sql.*

infix fun <T : Comparable<T>> Column<T>.openEndRange(range: OpenEndRange<T>): AndOp = AndOp(
    listOf(
        GreaterEqOp(this, QueryParameter(range.start, columnType)),
        LessOp(this, QueryParameter(range.endExclusive, columnType))
    )
)

infix fun <T : Comparable<T>> ClosedRange<Column<T>>.overlap(valueRange: ClosedRange<T>): AndOp = AndOp(
    listOf(
        LessEqOp(start, QueryParameter(valueRange.endInclusive, start.columnType)),
        GreaterEqOp(endInclusive, QueryParameter(valueRange.start, endInclusive.columnType))
    )
)

infix fun List<Column<*>>.inList(values: List<List<Any>>): Op<Boolean> {
    val columns = this
    return object : Op<Boolean>() {
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
}