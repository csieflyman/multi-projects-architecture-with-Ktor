/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed.jasync

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.SuspendingConnection
import com.github.jasync.sql.db.asSuspending
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.name
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.util.concurrent.ConcurrentHashMap

object JasyncExposedAdapter {

    private val connectionMap = ConcurrentHashMap<Database, SuspendingConnection>()

    fun bind(database: Database, connection: Connection) {
        connectionMap[database] = connection.asSuspending
    }

    fun currentExposedTransaction() = TransactionManager.current()

    private fun currentExposedDatabase() = currentExposedTransaction().db

    fun currentJasyncConnection(): SuspendingConnection = jasyncConnection(currentExposedDatabase())

    fun jasyncConnection(database: Database): SuspendingConnection = database.let {
        connectionMap[it] ?: error("No jasync connection bind with ${it.name}.")
    }
}