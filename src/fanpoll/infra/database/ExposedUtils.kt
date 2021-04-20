/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */
package fanpoll.infra.database

import com.google.common.base.CaseFormat
import fanpoll.infra.InternalServerErrorException
import fanpoll.infra.ResponseCode
import fanpoll.infra.auth.PrincipalSource
import fanpoll.infra.auth.UserType
import fanpoll.infra.utils.CustomNameEnum
import fanpoll.infra.utils.CustomNameEnumConverter
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Function
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.vendors.currentDialect
import java.time.*
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

private val profilingLogger = KotlinLogging.logger("fanpoll.infra.database.Profiling")

object ExposedUtils {

    fun multipleColumnInClauseExpression(columns: List<Column<Any>>, values: List<List<Any>>) = object : Op<Boolean>() {
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

fun <T> myTransaction(db: Database? = null, statement: Transaction.() -> T): T {
    return transaction(db) {
        val begin = Instant.now()
        profilingLogger.debug { "===== Transaction Profiling Begin ($id) ===== " }
        try {
            statement()
        } catch (e: ExposedSQLException) {
            throw InternalServerErrorException(ResponseCode.DB_SQL_ERROR, e.toString(), e) // include caused SQL
        } finally {
            profilingLogger.debug {
                "===== Transaction execution time: ${
                    Duration.between(begin, Instant.now()).toMillis()
                } millis ($id) ====="
            }
        }
    }
}

// ========== Extension(Column) ==========
val Column<*>.propName: String
    get() = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, this.name)

// ========== Extension(ResultRow) ==========

fun ResultRow.getPKValueOfTable(table: Table): Any? = this[table.primaryKey!!.columns.single()]

fun ResultRow.getPKValuesOfTable(table: Table): List<Any>? = table.primaryKey!!.columns.filter { this.hasValue(it) }
    .onEach { logger.debug { "$it  => ${this[it]}" } }
    .map { column -> (this[column] as? EntityID<*>)?.value ?: this[column] }.filterNotNull()
    .toList().takeIf { it.isNotEmpty() }

// ========== Extension( Table: Custom Column Type) ==========

fun <E : Enum<E>> Table.customNameEnumeration(
    name: String, length: Int, klass: KClass<E>,
    converter: CustomNameEnumConverter
): Column<CustomNameEnum<E>> =
    registerColumn(name, object : VarCharColumnType(length) {
        override fun valueFromDB(value: Any): CustomNameEnum<E> = when (value) {
            is String -> CustomNameEnum(value, klass, converter)
            is Enum<*> -> CustomNameEnum(value as E, converter)
            else -> error("$value of ${value::class.qualifiedName} is not valid for enum ${klass.qualifiedName}")
        }

        override fun notNullValueToDB(value: Any): Any = converter.toCustomName
    })

fun Table.json(name: String): Column<JsonElement> = registerColumn(name, object : StringColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
    override fun valueFromDB(value: Any): JsonElement = fanpoll.infra.utils.json.parseToJsonElement(value as String)
    override fun notNullValueToDB(value: Any): Any = value.toString()
})

fun Table.jsonObject(name: String): Column<JsonObject> = registerColumn(name, object : StringColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
    override fun valueFromDB(value: Any): JsonElement =
        fanpoll.infra.utils.json.parseToJsonElement(value as String).jsonObject

    override fun notNullValueToDB(value: Any): Any = value.toString()
})

fun Table.jsonArray(name: String): Column<JsonArray> = registerColumn(name, object : StringColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
    override fun valueFromDB(value: Any): JsonElement =
        fanpoll.infra.utils.json.parseToJsonElement(value as String).jsonArray

    override fun notNullValueToDB(value: Any): Any = value.toString()
})

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> Table.dto(name: String): Column<T> =
    registerColumn(name, object : StringColumnType() {
        override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
        override fun valueFromDB(value: Any): T =
            fanpoll.infra.utils.json.decodeFromString(T::class.serializer(), value as String)

        override fun notNullValueToDB(value: Any): Any =
            fanpoll.infra.utils.json.encodeToString(T::class.serializer(), value as T)
    })

@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> Table.dtoList(name: String): Column<List<T>> =
    registerColumn(name, object : StringColumnType() {
        override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
        override fun valueFromDB(value: Any): List<T> =
            fanpoll.infra.utils.json.decodeFromString(ListSerializer(T::class.serializer()), value as String)

        override fun notNullValueToDB(value: Any): Any =
            fanpoll.infra.utils.json.encodeToString(ListSerializer(T::class.serializer()), value as List<T>)
    })

fun Table.principalSource(name: String, length: Int): Column<PrincipalSource> =
    registerColumn(name, object : VarCharColumnType(length) {
        override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
        override fun valueFromDB(value: Any): PrincipalSource = PrincipalSource.lookup(value as String)
        override fun notNullValueToDB(value: Any): Any = (value as PrincipalSource).id
    })

fun Table.userType(name: String, length: Int): Column<UserType> =
    registerColumn(name, object : VarCharColumnType(length) {
        override fun sqlType(): String = currentDialect.dataTypeProvider.textType()
        override fun valueFromDB(value: Any): UserType = UserType.lookup(value as String)
        override fun notNullValueToDB(value: Any): Any = (value as UserType).id
    })

// ========== Extension(Operator) ==========

infix fun ExpressionWithColumnType<Instant>.inRange(pair: Pair<Instant, Instant>): AndOp = inTemporalRange(this, pair)

infix fun ExpressionWithColumnType<Instant>.inZonedDateTimeRange(pair: Pair<ZonedDateTime, ZonedDateTime>): AndOp =
    inTemporalRange(this, pair.first.toInstant() to pair.second.toInstant())

fun ExpressionWithColumnType<Instant>.inLocalDateTimeRange(
    start: LocalDateTime,
    endExclusive: LocalDateTime,
    zoneId: ZoneId
): AndOp =
    inTemporalRange(this, start.atZone(zoneId).toInstant() to endExclusive.atZone(zoneId).toInstant())

infix fun ExpressionWithColumnType<LocalDate>.inDateRange(pair: Pair<LocalDate, LocalDate>): AndOp =
    inTemporalRange(this, pair)

private fun <T, R> inTemporalRange(column: ExpressionWithColumnType<T>, pair: Pair<R, R>): AndOp =
    AndOp(
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
): AndOp =
    AndOp(
        listOf(
            LessEqOp(startColumn, QueryParameter(rangeEndInclusive, startColumn.columnType)),
            GreaterEqOp(endColumn, QueryParameter(rangeStart, endColumn.columnType))
        )
    )

// ========== Extension(Function) ==========

fun Function<*>.countDistinct(): Count = Count(this, true)

// ========== Extension(Expression) ==========

class RawExpression(private val raw: String) : Expression<String>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        +raw
    }
}

class CountExpression : Expression<Long>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        +"COUNT(*)"
    }
}

class NullableValueExpressionAlias<T : Any?>(private val exprAlias: ExpressionAlias<T>) : Expression<T?>() {

    override fun toQueryBuilder(queryBuilder: QueryBuilder) = exprAlias.toQueryBuilder(queryBuilder)
}

// Exposed 0.30.2 don't support operator on uuid column type. This is workaround solution
// org.postgresql.util.PSQLException: ERROR: operator does not exist: uuid = character varying
class PgSQLUUIDExpression(private val column: Column<*>) : Expression<String>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        +"CAST (${column.table.tableName}.${column.name} AS uuid)"
    }
}