/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.util

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMapError
import com.github.kittinunf.result.getOrNull
import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.exception.RequestException
import fanpoll.infra.base.query.DynamicQuery
import fanpoll.infra.base.response.DataResponseDTO
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.base.response.PagingDataResponseDTO
import fanpoll.infra.base.response.ResponseDTO
import fanpoll.infra.base.util.DateTimeUtils
import fanpoll.infra.database.dao.toDTO
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAccessor
import kotlin.reflect.KClass

inline fun <reified T : EntityDTO<*>> DynamicQuery.queryDB(): ResponseDTO {
    return transaction {
        if (offsetLimit != null && offsetLimit.isPaging) {
            val dbCountQuery = toDBCountQuery<T>()
            val total = dbCountQuery.count()
            val items = if (total > 0) {
                val dbQuery = toDBQuery<T>()
                dbQuery.toList<T>()
            } else listOf()
            PagingDataResponseDTO.dtoList(offsetLimit, total, items)
        } else {
            if (count == true) {
                val dbCountQuery = toDBCountQuery<T>()
                val total = dbCountQuery.count()
                DataResponseDTO(JsonObject(mapOf("total" to JsonPrimitive(total))))
            } else {
                val dbQuery = toDBQuery<T>()
                DataResponseDTO(dbQuery.toList<T>())
            }
        }
    }
}

inline fun <reified T : EntityDTO<*>> DynamicQuery.toDBQuery(): DynamicDBQuery<T> {
    return DynamicDBQuery(T::class, this, false)
}

inline fun <reified T : EntityDTO<*>> DynamicQuery.toDBCountQuery(): DynamicDBQuery<T> {
    return DynamicDBQuery(T::class, this, true)
}

fun <T : EntityDTO<*>> DynamicQuery.toDBQuery(dtoClass: KClass<T>): DynamicDBQuery<T> {
    return DynamicDBQuery(dtoClass, this, false)
}

class DynamicDBQuery<T : EntityDTO<*>>(dtoClass: KClass<T>, dynamicQuery: DynamicQuery, val count: Boolean) {

    private val mapper = ResultRowDTOMapper.getMapper(dtoClass)

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

    inline fun <reified T : EntityDTO<*>> toList(): List<T> {
        return query.toList().toDTO(T::class)
    }

    fun <T : EntityDTO<*>> toList(dtoClass: KClass<T>): List<T> {
        return query.toList().toDTO(dtoClass)
    }

    inline fun <reified T : EntityDTO<*>> toList(entityClass: EntityClass<*, *>): List<T> {
        return entityClass.wrapRows(query).toList().map { it.toDTO(T::class) }
    }

    private fun selectColumns(dynamicQuery: DynamicQuery) {
        if (!count) {
            if (dynamicQuery.fields != null && dynamicQuery.fields.isNotEmpty()) {
                query.adjustSlice {
                    val allColumns = linkedSetOf<Column<*>>()
                    val idColumns = mapper.table.primaryKey!!.columns
                    val fieldsColumns = dynamicQuery.fields.flatMap { mapper.getColumns(it) }
                    allColumns.addAll(idColumns)
                    allColumns.addAll(fieldsColumns)
                    slice(allColumns.toList())
                }
            } else {
                query.adjustSlice {
                    slice(mapper.columns)
                }
            }
        }
    }

    // ENHANCEMENT => support self join
    private fun joinTables(dynamicQuery: DynamicQuery) {
        val allFields = dynamicQuery.getAllFields()
        val relationTables = mapper.getRelationTables(allFields)
        //logger.debug { "relationTables = $relationTables" }
        if (relationTables.isNotEmpty()) {
            query.adjustColumnSet {
                run {
                    var columnSet: ColumnSet = this
                    relationTables.forEach { relTable ->
                        if (columnSet !is Join || (columnSet as? Join)?.alreadyInJoin(relTable) == false) {
                            val join = mapper.getJoin(relTable)
                            columnSet = with(join) {
                                columnSet.join(otherTable, joinType, onColumn, otherColumn, additionalConstraint)
                            }
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

        fun convertPredicate(predicate: DynamicQuery.Predicate, mapper: ResultRowDTOMapper<*>): Op<Boolean> {
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

        private fun convertPredicateValue(predicate: DynamicQuery.Predicate.Simple, columnType: IColumnType): Any? {
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

        private fun convertPredicateValue(field: String, value: Any, columnType: IColumnType): Any {
            return try {
                when (columnType) {
                    is IDateColumnType -> convertToDateTime(field, value, columnType) // Duration type is unsupported
                    is EnumerationColumnType<*> -> convertToEnum(field, value, columnType.klass)
                    else -> columnType.valueFromDB(value)
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

        // IDateColumnType subclass is internal (see org.jetbrains.exposed.sql.`java-time`.JavaDateColumnType)
        private fun convertToDateTime(field: String, value: Any, columnType: IColumnType): Any {
            return when (value) {
                is String -> columnType.valueFromDB(Result.of<TemporalAccessor, DateTimeParseException> {
                    ZonedDateTime.parse(value, DateTimeUtils.UTC_DATE_TIME_FORMATTER).toInstant()
                }.flatMapError {
                    Result.of { LocalDateTime.parse(value, DateTimeUtils.LOCAL_DATE_TIME_FORMATTER) }
                }.flatMapError {
                    Result.of { LocalDate.parse(value, DateTimeUtils.LOCAL_DATE_FORMATTER) }
                }.getOrNull() ?: throw RequestException(
                    InfraResponseCode.BAD_REQUEST_QUERYSTRING, "invalid query filter: field $field value is invalid for datetime => $value"
                )
                )
                else -> columnType.valueFromDB(value)
            }
        }
    }
}

class DynamicDBJoinPart(
    val joinType: JoinType,
    val otherTable: ColumnSet,
    val onColumn: Expression<*>,
    val otherColumn: Expression<*>,
    val additionalConstraint: (SqlExpressionBuilder.() -> Op<Boolean>)? = null
)