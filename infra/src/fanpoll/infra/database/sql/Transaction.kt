/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package fanpoll.infra.database.sql

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import mu.KotlinLogging
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant

private val profilingLogger = KotlinLogging.logger("fanpoll.infra.database.sql.Profiling")

fun <T> transaction(db: Database? = null, statement: Transaction.() -> T): T {
    return transaction(db) {
        val begin = Instant.now()
        profilingLogger.debug { "===== Transaction Profiling Begin ($id) ===== " }
        try {
            statement()
        } catch (e: ExposedSQLException) {
            throw InternalServerException(InfraResponseCode.DB_SQL_ERROR, e.toString(), e) // include caused SQL
        } finally {
            profilingLogger.debug {
                "===== Transaction execution time: ${Duration.between(begin, Instant.now()).toMillis()} millis ($id) ====="
            }
        }
    }
}