/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.sql

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.experimental.withSuspendTransaction
import kotlin.system.measureTimeMillis

// see https://github.com/JetBrains/Exposed/wiki/Transactions#using-nested-transactions
// see https://github.com/JetBrains/Exposed/wiki/Transactions#working-with-coroutines

private val profilingLogger = KotlinLogging.logger("fanpoll.infra.database.exposed.sql.Profiling")
suspend fun <T> dbExecute(db: Database? = null, statement: suspend Transaction.() -> T): T {
    val currentTransaction = TransactionManager.currentOrNull()
    return if (currentTransaction == null) {
        newSuspendedTransaction(Dispatchers.IO, db) {
            executeStatement(this, statement)
        }
    } else {
        currentTransaction.withSuspendTransaction {
            executeStatement(this, statement)
        }
    }
}

suspend fun <T> dbExecuteAsync(db: Database? = null, statement: suspend Transaction.() -> T): Deferred<T> {
    val currentTransaction = TransactionManager.currentOrNull()
    return if (currentTransaction == null) {
        suspendedTransactionAsync(Dispatchers.IO, db) {
            executeStatement(this, statement)
        }
    } else {
        currentTransaction.withSuspendTransaction {
            CompletableDeferred(executeStatement(this, statement))
        }
    }
}

private suspend fun <T> executeStatement(transaction: Transaction, statement: suspend Transaction.() -> T): T {
    var result: T
    val duration = measureTimeMillis {
        result = try {
            transaction.statement()
        } catch (e: ExposedSQLException) {
            throw InternalServerException(InfraResponseCode.DB_SQL_ERROR, e.toString(), e) // include caused SQL
        }
    }
    profilingLogger.debug { "===== Exposed transaction ${transaction.id} execution time: $duration milliseconds =====" }
    return result
}