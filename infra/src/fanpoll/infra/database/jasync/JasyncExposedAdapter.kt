/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.jasync

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.SuspendingConnection
import com.github.jasync.sql.db.asSuspending
import com.github.jasync.sql.db.exceptions.DatabaseException
import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.entity.EntityForm
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.sql.entityEq
import fanpoll.infra.database.sql.propName
import fanpoll.infra.database.sql.updateColumnMap
import fanpoll.infra.database.util.toDTO
import fanpoll.infra.database.util.toSingleDTO
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.JavaInstantColumnType
import org.jetbrains.exposed.sql.statements.DeleteStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

class JasyncExposedAdapter {

    companion object {

        private val connectionMap = ConcurrentHashMap<Database, SuspendingConnection>()

        lateinit var defaultDatabase: Database
        lateinit var defaultAsyncConnection: SuspendingConnection

        fun bind(database: Database, connection: Connection) {
            val suspendingConnection = connection.asSuspending
            if (connectionMap.isEmpty()) {
                defaultDatabase = database
                defaultAsyncConnection = suspendingConnection
            }
            connectionMap[database] = suspendingConnection
        }

        fun currentTransaction() = TransactionManager.current()

        private fun currentDatabase() = currentTransaction().db

        fun currentAsyncConnection(): SuspendingConnection = asyncConnection(currentDatabase())

        fun asyncConnection(database: Database): SuspendingConnection = database.let {
            connectionMap[it] ?: error("No jasync connection bind with ${it.name}.")
        }
    }
}

private val logger = KotlinLogging.logger {}

private val profilingLogger = KotlinLogging.logger("fanpoll.infra.database.jasync.Profiling")

suspend fun <T> dbQueryAsync(
    db: Database = JasyncExposedAdapter.defaultDatabase,
    statement: suspend Transaction.(SuspendingConnection) -> T
): Deferred<T> = suspendedTransactionAsync(Dispatchers.IO, db) {
    val begin = Instant.now()
    profilingLogger.debug { "===== dbQueryAsync Profiling Begin ($id) ===== " }
    try {
        statement(JasyncExposedAdapter.asyncConnection(db))
    } catch (e: ExposedSQLException) {
        throw InternalServerException(InfraResponseCode.DB_SQL_ERROR, e.toString(), e) // include caused SQL
    } catch (e: DatabaseException) {
        throw InternalServerException(InfraResponseCode.DB_JASYNC_ERROR, cause = e)
    } finally {
        profilingLogger.debug {
            "===== dbQueryAsync execution time: ${Duration.between(begin, Instant.now()).toMillis()} millis ($id) ====="
        }
    }
}

suspend fun <T> dbTransactionAsync(
    db: Database = JasyncExposedAdapter.defaultDatabase,
    statement: suspend Transaction.(SuspendingConnection) -> T
): T = JasyncExposedAdapter.asyncConnection(db).inTransaction { connection ->
    suspendedTransactionAsync(db = db) {
        val begin = Instant.now()
        profilingLogger.debug { "===== dbTransactionAsync Profiling Begin ($id) ===== " }
        try {
            statement(connection)
        } catch (e: ExposedSQLException) {
            throw InternalServerException(InfraResponseCode.DB_SQL_ERROR, e.toString(), e) // include caused SQL
        } catch (e: DatabaseException) {
            throw InternalServerException(InfraResponseCode.DB_JASYNC_ERROR, cause = e)
        } finally {
            profilingLogger.debug {
                "===== dbTransactionAsync execution time: ${Duration.between(begin, Instant.now()).toMillis()} millis ($id) ====="
            }
        }
    }.await()
}

suspend inline fun <reified T : EntityDTO<*>> Query.toSingleDTOOrNull(): T? =
    await().toSingleDTOOrNull(this, T::class)

suspend inline fun <reified T : EntityDTO<*>> Query.toDTO(): List<T> =
    await().toDTO(this, T::class)

suspend fun Query.await(): QueryResult {
    val connection = JasyncExposedAdapter.currentAsyncConnection()
    val transaction = JasyncExposedAdapter.currentTransaction()
    val sql = prepareSQL(QueryBuilder(true))
    val args = getStatementArgs(this)
    logger.debug { "[jasync] (${transaction.id}) $sql => args = $args" }
    return connection.sendPreparedStatement(sql, args)
        .also { logger.debug { "[jasync] (${transaction.id}) $it" } }
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

suspend fun <T> T.insertAsync(
    form: EntityForm<*, *, *>,
    body: (T.(InsertStatement<Number>) -> Unit)? = null
): Long where T : Table, T : fanpoll.infra.database.sql.Table<*> {
    val connection = JasyncExposedAdapter.currentAsyncConnection()
    val transaction = JasyncExposedAdapter.currentTransaction()

    val table = this
    val statement = InsertStatement<Number>(table).apply {
        val dtoProps = form::class.memberProperties
        (columns as List<Column<Any>>).forEach { column ->
            dtoProps.find { dtoProp -> autoIncColumn != column && dtoProp.name == column.propName }
                ?.let { dtoProp -> dtoProp.getter.call(form)?.let { this[column] = it } }
        }
        body?.invoke(table, this)
    }

    return executeInTxAsync(connection, transaction, statement)
}

suspend fun <T> T.updateAsync(
    form: EntityForm<*, *, *>,
    body: (T.(UpdateStatement) -> Unit)? = null
): Long where T : Table, T : fanpoll.infra.database.sql.Table<*> {
    val connection = JasyncExposedAdapter.currentAsyncConnection()
    val transaction = JasyncExposedAdapter.currentTransaction()

    val table = this
    val updateColumnMap = updateColumnMap()
    val statement = UpdateStatement(table, null, SqlExpressionBuilder.entityEq(table, form)).apply {
        form::class.memberProperties.forEach { dtoProp ->
            updateColumnMap[dtoProp.name]?.let { column ->
                dtoProp.getter.call(form)?.let { dtoPropValue -> this[column] = dtoPropValue }
            }
        }
        body?.invoke(table, this)
    }

    return executeInTxAsync(connection, transaction, statement)
}

suspend fun <T> T.deleteAsync(
    op: SqlExpressionBuilder.() -> Op<Boolean>
): Long where T : Table, T : fanpoll.infra.database.sql.Table<*> {
    val connection = JasyncExposedAdapter.currentAsyncConnection()
    val transaction = JasyncExposedAdapter.currentTransaction()

    val table = this
    val statement = DeleteStatement(table, SqlExpressionBuilder.op())

    return executeInTxAsync(connection, transaction, statement)
}

private suspend fun executeInTxAsync(connection: SuspendingConnection, transaction: Transaction, statement: Statement<*>): Long {
    val sql = statement.prepareSQL(transaction)
    val args = getStatementArgs(statement)
    logger.debug { "[jasync] (${transaction.id}) $sql => args = $args" }
    return connection.sendPreparedStatement(sql, args)
        .also { logger.debug { "[jasync] (${transaction.id}) $it" } }
        .rowsAffected
}

private fun getStatementArgs(statement: Statement<*>): List<Any?> = when (statement) {
    is InsertStatement<*> -> statement.arguments().takeIf { it.isNotEmpty() }?.let { it[0] }
    else -> statement.arguments().takeIf { it.any() }?.first()
}?.map { pair -> pair.second?.let { pair.first.notNullValueToDB(it) } }?.toList() ?: emptyList()