/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.jasync

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.exceptions.DatabaseException
import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.util.toDTO
import fanpoll.infra.database.util.toSingleDTO
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import mu.KotlinLogging
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.JavaInstantColumnType
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class JasyncExposedAdapter {

    companion object {

        private val bindConnections = ConcurrentHashMap<Database, Connection>()

        fun bind(database: Database, connection: Connection) {
            bindConnections[database] = connection
        }

        fun current(): Connection = connection(TransactionManager.current().db)

        fun connection(database: Database): Connection = database.let {
            bindConnections[it] ?: error("No jasync connection bind with ${it.name}.")
        }
    }
}

private val logger = KotlinLogging.logger {}

private val profilingLogger = KotlinLogging.logger("fanpoll.infra.database.jasync.Profiling")

fun <T> dbQueryAsync(db: Database? = null, statement: Transaction.(Connection) -> CompletableFuture<T>): Deferred<T> =
    dbExecuteAsync(db, false, statement)

fun <T> dbTransactionAsync(db: Database? = null, statement: Transaction.(Connection) -> CompletableFuture<T>): Deferred<T> =
    dbExecuteAsync(db, true, statement)

private fun <T> dbExecuteAsync(
    db: Database? = null,
    inTransaction: Boolean,
    statement: Transaction.(Connection) -> CompletableFuture<T>
): Deferred<T> {
    return transaction(db) {
        val begin = Instant.now()
        profilingLogger.debug { "===== Transaction Profiling Begin ($id) ===== " }
        try {
            val connection = JasyncExposedAdapter.connection(db ?: TransactionManager.current().db)
            if (inTransaction)
                connection.inTransaction { statement(connection) }
            else
                statement(connection)
        } catch (e: ExposedSQLException) {
            throw InternalServerException(InfraResponseCode.DB_SQL_ERROR, e.toString(), e) // include caused SQL
        } catch (e: DatabaseException) {
            throw InternalServerException(InfraResponseCode.DB_JASYNC_ERROR, cause = e)
        } finally {
            profilingLogger.debug {
                "===== Transaction execution time: ${Duration.between(begin, Instant.now()).toMillis()} millis ($id) ====="
            }
        }
    }.asDeferred()
}

inline fun <reified T : EntityDTO<*>> Query.toSingleDTOFuture(): CompletableFuture<T?> =
    toFuture().thenApply { it.toSingleDTOOrNull(this, T::class) }

inline fun <reified T : EntityDTO<*>> Query.toDTOFuture(): CompletableFuture<List<T>> =
    toFuture().thenApply { it.toDTO(this, T::class) }

fun Query.toFuture(): CompletableFuture<QueryResult> {
    val sql = prepareSQL(QueryBuilder(true))
    val args = arguments().takeIf { it.isNotEmpty() }?.let { it[0] }?.map { it.second }?.toList() ?: emptyList()
    logger.debug { "[jasync] $sql => args = $args" }
    return JasyncExposedAdapter.current().sendPreparedStatement(sql, args)
}

fun <T : EntityDTO<*>> QueryResult.toSingleDTOOrNull(query: Query, dtoClass: KClass<T>): T? =
    toResultRows(query).toSingleDTO(dtoClass)

fun <T : EntityDTO<*>> QueryResult.toDTO(query: Query, dtoClass: KClass<T>): List<T> =
    toResultRows(query).toDTO(dtoClass)

private fun QueryResult.toResultRows(query: Query): List<ResultRow> {
    val fieldsIndex = query.set.realFields.toSet().mapIndexed { index, expression -> expression to index }.toMap()
    return rows.map { rowData ->
        val data = fieldsIndex.map { (field, index) -> field to convertValue(field, rowData[index]) }.toMap()
        ResultRow.createAndFillValues(data)
    }
}

private fun convertValue(field: Expression<*>, value: Any?): Any? {
    return if (value != null && field is Column<*>) {
        when {
            // In jasync, PostgreSQL TIMESTAMP without time zone mapped to LocalDateTime
            // but Exposed JavaInstantColumnType can't convert it to Instant
            (value is LocalDateTime && field.columnType is JavaInstantColumnType) -> value.atZone(ZoneId.systemDefault()).toInstant()
            else -> value
        }
    } else value
}