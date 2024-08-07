/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.util

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMapError
import com.github.kittinunf.result.getOrNull
import fanpoll.infra.base.datetime.DateTimeUtils
import fanpoll.infra.base.entity.Identifiable
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.query.DynamicQuery
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.exposed.dao.toObject
import fanpoll.infra.database.exposed.sql.toList
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAccessor
import kotlin.reflect.KClass

class DynamicDBQuery<T : Any>(private val objectClass: KClass<T>, dynamicQuery: DynamicQuery, val count: Boolean) {

    private val mapper = ResultRowMappers.getMapper(objectClass)!!

    val query: Query = Query(mapper.table, null)

    init {
        selectColumns(dynamicQuery)
        joinTables(dynamicQuery)
        where(dynamicQuery)
        orderBy(dynamicQuery)
        offsetLimit(dynamicQuery)
    }

    fun count(): Long {
        return query.count()
    }

    fun toResultRows(): List<ResultRow> {
        return query.toList()
    }

    fun toList(): List<T> {
        return query.toList(objectClass)
    }

    inline fun <reified T : Identifiable<*>> toList(entityClass: EntityClass<*, *>): List<T> {
        return entityClass.wrapRows(query).toList().map { it.toObject(T::class) }
    }

    private fun selectColumns(dynamicQuery: DynamicQuery) {
        if (!count) {
            if (!dynamicQuery.fields.isNullOrEmpty()) {
                query.adjustSelect {
                    val allColumns = linkedSetOf<Column<*>>()
                    val idColumns = mapper.table.primaryKey!!.columns
                    val fieldsColumns = dynamicQuery.fields.flatMap { mapper.getColumns(it) }
                    val relationTables = mapper.getTables(dynamicQuery.fields)
                    val relationTableIdColumns = relationTables.flatMap { it.primaryKey!!.columns.toList() }
                    allColumns.addAll(idColumns)
                    allColumns.addAll(relationTableIdColumns)
                    allColumns.addAll(fieldsColumns)
                    select(allColumns.toList())
                }
            } else {
                query.adjustSelect {
                    select(columns)
                }
            }
        }
    }

    // ENHANCEMENT => support self join
    private fun joinTables(dynamicQuery: DynamicQuery) {
        val allFields = dynamicQuery.getAllFields()
        val relationTables = mapper.getTables(allFields)
        //logger.debug { "relationTables = $relationTables" }
        if (relationTables.isNotEmpty()) {
            query.adjustColumnSet {
                run {
                    var columnSet: ColumnSet = this
                    relationTables.forEach { relTable ->
                        if (columnSet !is Join || (columnSet as? Join)?.alreadyInJoin(relTable) == false) {
                            val joinType = mapper.getJoinType(relTable)
                            columnSet = columnSet.join(relTable, joinType)
                        }
                    }
                    columnSet
                }
            }
        }
    }

    // case Simple: a = 1
    // case Junction: a = 1 or b = 2 or c = 3
    // case nested Junction: (a = 1) and (b = 2 or c = 3) and ((d = 4) or (e = 5 and f = 6))
    private fun where(dynamicQuery: DynamicQuery) {
        if (dynamicQuery.filter != null) {
            query.adjustWhere { convertPredicate(dynamicQuery.filter, mapper) }
        }
    }

    private fun orderBy(dynamicQuery: DynamicQuery) {
        if (!count && !dynamicQuery.orderByList.isNullOrEmpty()) {
            query.orderBy(*dynamicQuery.orderByList.map {
                mapper.getColumn(it.field) to (if (it.asc == null || it.asc) SortOrder.ASC else SortOrder.DESC)
            }.toTypedArray())
        }
    }

    private fun offsetLimit(dynamicQuery: DynamicQuery) {
        if (!count && dynamicQuery.offsetLimit != null) {
            query.limit(dynamicQuery.offsetLimit.limit, dynamicQuery.offsetLimit.offset)
        }
    }

    companion object {

        private val logger = KotlinLogging.logger {}

        fun convertPredicate(predicate: DynamicQuery.Predicate, mapper: ResultRowMapper<*>): Op<Boolean> {
            return when (predicate) {
                is DynamicQuery.Predicate.Simple -> {
                    convertSimplePredicate(predicate, mapper.getColumn(predicate.field))
                }

                is DynamicQuery.Predicate.Junction -> {
                    val dbPredicates = predicate.children.map { convertPredicate(it, mapper) }
                    if (predicate.isConjunction) AndOp(dbPredicates) else OrOp(dbPredicates)
                }
            }
        }

        private fun convertSimplePredicate(predicate: DynamicQuery.Predicate.Simple, column: Column<*>): Op<Boolean> {
            val value: Any? = convertPredicateValue(predicate, column.columnType)
            return when (predicate.operator) {
                DynamicQuery.PredicateOperator.EQ -> Op.build { (column as Column<Any>) eq value!! }
                DynamicQuery.PredicateOperator.NEQ -> Op.build { (column as Column<Any>) neq value!! }
                DynamicQuery.PredicateOperator.LIKE -> Op.build { (column as Column<String>) like (value!! as String) }

                DynamicQuery.PredicateOperator.LT -> Op.build { column less (value!! as Comparable<Any>) }
                DynamicQuery.PredicateOperator.LE -> Op.build { column lessEq (value!! as Comparable<Any>) }
                DynamicQuery.PredicateOperator.GT -> Op.build { column greater (value!! as Comparable<Any>) }
                DynamicQuery.PredicateOperator.GE -> Op.build { column greaterEq (value!! as Comparable<Any>) }

                DynamicQuery.PredicateOperator.IN -> Op.build { (column as Column<Any>) inList (value!! as Iterable<Any>) }
                DynamicQuery.PredicateOperator.NOT_IN -> Op.build { (column as Column<Any>) notInList (value!! as Iterable<Any>) }

                DynamicQuery.PredicateOperator.IS_NULL -> Op.build { (column as Column<Any?>).isNull() }
                DynamicQuery.PredicateOperator.IS_NOT_NULL -> Op.build { (column as Column<Any?>).isNotNull() }
            }
        }

        private fun convertPredicateValue(predicate: DynamicQuery.Predicate.Simple, columnType: IColumnType<*>): Any? {
            return when (predicate.operator.valueKind) {
                DynamicQuery.PredicateValueKind.NONE -> null
                DynamicQuery.PredicateValueKind.SINGLE -> convertPredicateValue(
                    predicate.field,
                    predicate.value!!,
                    columnType
                )

                DynamicQuery.PredicateValueKind.MULTIPLE -> (predicate.value as Iterable<*>)
                    .map { convertPredicateValue(predicate.field, it!!, columnType) }
            }
        }

        private fun convertPredicateValue(field: String, value: Any, columnType: IColumnType<*>): Any {
            return try {
                when (columnType) {
                    is IDateColumnType -> convertToDateTime(field, value, columnType) // Duration type is unsupported
                    is EnumerationColumnType<*> -> convertToEnum(field, value, columnType.klass)
                    else -> columnType.valueFromDB(value)!!
                }
            } catch (e: Exception) {
                if (e is RequestException) throw e
                else throw RequestException(
                    InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                    "invalid query filter: field $field value is invalid => $value", e
                )
            }
        }

        private fun <T : Enum<T>> convertToEnum(field: String, value: Any, klass: KClass<T>): T {
            return when (value) {
                is String -> klass.java.enumConstants!!.first { it.name == value }
                is Number -> klass.java.enumConstants!![value.toInt()]
                else -> throw RequestException(
                    InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                    "invalid query filter: field $field value is invalid for enum => $value"
                )
            }
        }

        // IDateColumnType subclass is internal (see org.jetbrains.exposed.sql.javatime.JavaDateColumnType)
        private fun convertToDateTime(field: String, value: Any, columnType: IColumnType<*>): Any {
            return when (value) {
                is String -> columnType.valueFromDB(Result.of<TemporalAccessor, DateTimeParseException> {
                    ZonedDateTime.parse(value, DateTimeUtils.UTC_DATE_TIME_FORMATTER).toInstant()
                }.flatMapError {
                    Result.of { LocalDateTime.parse(value, DateTimeUtils.LOCAL_DATE_TIME_FORMATTER) }
                }.flatMapError {
                    Result.of { LocalDate.parse(value, DateTimeUtils.LOCAL_DATE_FORMATTER) }
                }.getOrNull() ?: throw RequestException(
                    InfraResponseCode.BAD_REQUEST_QUERYSTRING,
                    "invalid query filter: field $field value is invalid for datetime => $value"
                )
                )!!

                else -> columnType.valueFromDB(value)!!
            }
        }
    }
}