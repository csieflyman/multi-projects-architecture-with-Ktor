/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.jasync

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.exceptions.DatabaseException
import fanpoll.infra.base.entity.EntityDTO
import fanpoll.infra.base.entity.EntityForm
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.sql.entityEq
import fanpoll.infra.database.sql.propName
import fanpoll.infra.database.util.toDTO
import fanpoll.infra.database.util.toSingleDTO
import mu.KotlinLogging
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.JavaInstantColumnType
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

class JasyncExposedAdapter {

    companion object {

        private val connectionMap = ConcurrentHashMap<Database, Connection>()

        lateinit var defaultDatabase: Database
        lateinit var defaultAsyncConnection: Connection

        fun bind(database: Database, connection: Connection) {
            if (connectionMap.isEmpty()) {
                defaultDatabase = database
                defaultAsyncConnection = connection
            }
            connectionMap[database] = connection
        }

        fun currentTransaction() = TransactionManager.current()

        private fun currentDatabase() = currentTransaction().db

        fun currentAsyncConnection() = asyncConnection(currentDatabase())

        fun asyncConnection(database: Database): Connection = database.let {
            connectionMap[it] ?: error("No jasync connection bind with ${it.name}.")
        }
    }
}

private val logger = KotlinLogging.logger {}

private val profilingLogger = KotlinLogging.logger("fanpoll.infra.database.jasync.Profiling")

fun <T> dbQueryAsync(
    db: Database = JasyncExposedAdapter.defaultDatabase,
    statement: Transaction.(Connection) -> CompletableFuture<T>
): CompletableFuture<T> {
    return transaction(db) {
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
}

fun <T> dbTransactionAsync(
    db: Database = JasyncExposedAdapter.defaultDatabase,
    statement: Transaction.(Connection) -> CompletableFuture<T>
): CompletableFuture<T> {
    return JasyncExposedAdapter.asyncConnection(db).inTransaction {
        val connection = it
        transaction(db) {
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
        }
    }
}

inline fun <reified T : EntityDTO<*>> Query.toSingleDTOFuture(): CompletableFuture<T?> =
    toFuture().thenApply { it.toSingleDTOOrNull(this, T::class) }

inline fun <reified T : EntityDTO<*>> Query.toDTOFuture(): CompletableFuture<List<T>> =
    toFuture().thenApply { it.toDTO(this, T::class) }

fun Query.toFuture(): CompletableFuture<QueryResult> {
    val sql = prepareSQL(QueryBuilder(true))
    val args = arguments().takeIf { it.isNotEmpty() }?.let { it[0] }?.map { it.second }?.toList() ?: emptyList()
    logger.debug { "[jasync query] $sql => args = $args" }
    return JasyncExposedAdapter.currentAsyncConnection().sendPreparedStatement(sql, args)
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

fun <T> T.insertAsync(
    form: EntityForm<*, *, *>,
    body: (T.(InsertStatement<Number>) -> Unit)? = null
): CompletableFuture<Any?> where T : Table, T : fanpoll.infra.database.sql.Table<*> {
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
    val sql = statement.prepareSQL(transaction)
    val args = statement.arguments().takeIf { it.isNotEmpty() }?.let { it[0] }?.map { it.second }?.toList() ?: emptyList()
    logger.debug { "[jasync insert] $sql => args = $args" }
    return connection.sendPreparedStatement(sql, args).thenApply { resultSet ->
        resultSet.rows.takeIf { it.isNotEmpty() }?.let { it[0][0] }
            ?.also { logger.debug { "resultSet value = $it" } }
    }
}

fun <T> T.updateAsync(
    form: EntityForm<*, *, *>,
    body: (T.(UpdateStatement) -> Unit)? = null
): CompletableFuture<Any?> where T : Table, T : fanpoll.infra.database.sql.Table<*> {
    val connection = JasyncExposedAdapter.currentAsyncConnection()
    val transaction = JasyncExposedAdapter.currentTransaction()

    val pkColumns = primaryKey!!.columns.toList() as List<Column<Any>>
    val columnMap = columns.associateBy { it.propName } as Map<String, Column<Any>>
    val updateColumnMap = columnMap.filterKeys { it != "createdAt" && it != "updatedAt" }
        .filterValues { !pkColumns.contains(it) }

    val table = this
    val statement = UpdateStatement(table, null, SqlExpressionBuilder.entityEq(table, form)).apply {
        form::class.memberProperties.forEach { dtoProp ->
            updateColumnMap[dtoProp.name]?.let { column ->
                dtoProp.getter.call(form)?.let { dtoPropValue -> this[column] = dtoPropValue }
            }
        }
        body?.invoke(table, this)
    }

    val sql = statement.prepareSQL(transaction)
    val args = statement.arguments().takeIf { it.any() }?.first()?.map { it.second }?.toList() ?: emptyList()
    logger.debug { "[jasync update] $sql => args = $args" }
    return connection.sendPreparedStatement(sql, args).thenApply { resultSet ->
        resultSet.rows.takeIf { it.isNotEmpty() }?.let { it[0][0] }
            ?.also { logger.debug { "resultSet value = $it" } }
    }
}