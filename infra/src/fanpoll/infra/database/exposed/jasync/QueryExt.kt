/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.jasync

import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.SuspendingConnection
import com.github.jasync.sql.db.exceptions.DatabaseException
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.exposed.sql.argValues
import fanpoll.infra.database.exposed.sql.toObject
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.JavaInstantColumnType
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}
private val profilingLogger = KotlinLogging.logger("fanpoll.infra.database.exposed.jasync.Profiling")
suspend fun <T> jasyncQuery(
    db: Database,
    statement: suspend Transaction.(SuspendingConnection) -> T
): Deferred<T> = suspendedTransactionAsync(Dispatchers.IO, db) {
    val connection = JasyncExposedAdapter.jasyncConnection(db)
    var result: T
    val duration = measureTimeMillis {
        result = try {
            statement(connection)
        } catch (e: ExposedSQLException) {
            throw InternalServerException(InfraResponseCode.DB_SQL_ERROR, e.toString(), e) // include caused SQL
        } catch (e: DatabaseException) {
            throw InternalServerException(InfraResponseCode.DB_JASYNC_ERROR, cause = e)
        }
    }
    profilingLogger.debug { "===== jasyncQuery $id execution time: $duration millis =====" }
    result
}

suspend fun <T : Any> Query.jasyncSingleDTOOrNull(objClass: KClass<T>): T? {
    return jasyncToList().getOrNull(0)?.toObject(objClass)
}

suspend fun <T : Any> Query.jasyncToList(objClass: KClass<T>): List<T> {
    return jasyncToList().toObject(objClass)
}

suspend fun Query.jasyncSingleDTOOrNull(): ResultRow? {
    return jasyncToList().getOrNull(0)
}

suspend fun Query.jasyncToList(): List<ResultRow> {
    val sql = prepareSQL(QueryBuilder(true))
    val args = argValues()
    val queryResult = executeJasyncQuery(sql, args)
    return convertJasyncQueryResultToExposedResultRows(queryResult, this)
}

private suspend fun executeJasyncQuery(sql: String, args: List<Any?>): QueryResult {
    val connection = JasyncExposedAdapter.currentJasyncConnection()
    val transaction = JasyncExposedAdapter.currentExposedTransaction()
    logger.debug { "[jasync] (${transaction.id}) $sql => args = $args" }
    return connection.sendPreparedStatement(sql, args)
        .also { logger.debug { "[jasync] (${transaction.id}) $it" } }
}

private fun convertJasyncQueryResultToExposedResultRows(jasyncQueryResult: QueryResult, exposedQuery: Query): List<ResultRow> {
    val fieldsIndex = exposedQuery.set.realFields.toSet().mapIndexed { index, expression -> expression to index }.toMap()
    return jasyncQueryResult.rows.map { rowData ->
        val data = fieldsIndex.map { (field, index) -> field to convertJasyncDataValue(field, rowData[index]) }.toMap()
        ResultRow.createAndFillValues(data)
    }
}

private fun convertJasyncDataValue(field: Expression<*>, value: Any?): Any? {
    return if (value != null && field is Column<*>) {
        when {
            // In jasync, PostgreSQL TIMESTAMP without time zone mapped to LocalDateTime
            // but Exposed JavaInstantColumnType can't convert it to Instant
            (value is LocalDateTime && field.columnType is JavaInstantColumnType) -> value.atZone(ZoneId.systemDefault()).toInstant()
            else -> value
        }
    } else value
}