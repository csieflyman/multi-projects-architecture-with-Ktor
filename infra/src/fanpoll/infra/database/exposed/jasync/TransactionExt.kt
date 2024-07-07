/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.jasync

import com.github.jasync.sql.db.SuspendingConnection
import com.github.jasync.sql.db.exceptions.DatabaseException
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.exposed.sql.argValues
import fanpoll.infra.database.exposed.sql.ignoreUpdateColumns
import fanpoll.infra.database.exposed.sql.propName
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.DeleteStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.experimental.withSuspendTransaction
import kotlin.reflect.full.memberProperties
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}
private val profilingLogger = KotlinLogging.logger("fanpoll.infra.database.exposed.jasync.Profiling")

suspend fun <T> jasyncTransaction(
    db: Database,
    statement: suspend Transaction.(SuspendingConnection) -> T
): Deferred<T> = suspendedTransactionAsync(Dispatchers.IO, db = db) {
    val exposedTransaction = this
    JasyncExposedAdapter.jasyncConnection(db).inTransaction { connection ->
        var result: T
        val duration = measureTimeMillis {
            result = try {
                exposedTransaction.withSuspendTransaction {
                    statement(connection)
                }
            } catch (e: ExposedSQLException) {
                throw InternalServerException(InfraResponseCode.DB_SQL_ERROR, e.toString(), e) // include caused SQL
            } catch (e: DatabaseException) {
                throw InternalServerException(InfraResponseCode.DB_JASYNC_ERROR, cause = e)
            }
        }
        profilingLogger.debug { "===== jasyncTransaction $id execution time: $duration millis =====" }
        result
    }
}


suspend fun <ID : Comparable<ID>> IdTable<ID>.jasyncInsert(
    obj: Any,
    body: (Table.(InsertStatement<Number>) -> Unit)? = null
) {
    val table = this
    val statement = InsertStatement<Number>(table).apply {
        fillInsertColumns(obj, this)
        body?.invoke(table, this)
    }
    executeStatement(statement)
}

private fun <ID : Comparable<ID>> IdTable<ID>.fillInsertColumns(obj: Any, insertStatement: InsertStatement<*>) {
    (columns as List<Column<Any>>).forEach { column ->
        obj::class.memberProperties.find { objProp -> autoIncColumn != column && objProp.name == column.propName }
            ?.let { objProp ->
                objProp.getter.call(obj)?.let {
                    if (column.columnType is EntityIDColumnType<*>)
                        insertStatement[column] = EntityID(it as ID, this)
                    else
                        insertStatement[column] = it
                }
            }
    }
}

suspend fun Table.jasyncUpdate(
    obj: Any,
    where: Op<Boolean>? = null,
    body: (Table.(UpdateStatement) -> Unit)? = null
): Long {
    val table = this
    val ignoreUpdateColumns = ignoreUpdateColumns()
    val updateColumnMap = columns.filterNot { ignoreUpdateColumns.contains(it) }
        .associateBy { it.propName } as Map<String, Column<Any>>
    val statement = UpdateStatement(table, null, where).apply {
        obj::class.memberProperties.forEach { objProp ->
            updateColumnMap[objProp.name]?.let { column ->
                objProp.getter.call(obj)?.let { objPropValue -> this[column] = objPropValue }
            }
        }
        body?.invoke(table, this)
    }

    return executeStatement(statement)
}

suspend fun Table.jasyncDelete(
    op: SqlExpressionBuilder.() -> Op<Boolean>
): Long {
    val table = this
    val statement = DeleteStatement(table, SqlExpressionBuilder.op())

    return executeStatement(statement)
}

private suspend fun executeStatement(statement: Statement<*>): Long {
    val connection = JasyncExposedAdapter.currentJasyncConnection()
    val transaction = JasyncExposedAdapter.currentExposedTransaction()
    val sql = statement.prepareSQL(transaction)
    val args = statement.argValues()
    logger.debug { "[jasync] (${transaction.id}) $sql => args = $args" }
    return connection.sendPreparedStatement(sql, args)
        .also { logger.debug { "[jasync] (${transaction.id}) $it" } }
        .rowsAffected
}