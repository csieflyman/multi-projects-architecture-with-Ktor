/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

object ExposedDatabaseManager {

    private val logger = KotlinLogging.logger {}

    private val databases = ConcurrentHashMap<String, Database>()
    private lateinit var defaultDatabase: Database

    @Synchronized
    fun createDatabase(dbName: String, isDefault: Boolean, dataSource: DataSource, externalConfig: ExposedDatabaseConfig): Database {
        if (databases.containsKey(dbName))
            throw InternalServerException(
                InfraResponseCode.SERVER_CONFIG_ERROR,
                "duplicated exposed database name: $dbName"
            )
        if (this::defaultDatabase.isInitialized && isDefault)
            throw InternalServerException(
                InfraResponseCode.SERVER_CONFIG_ERROR,
                "exposed default database had been assigned: $dbName"
            )

        val config = buildExposedDatabaseConfig(externalConfig)
        try {
            val database = Database.connect(dataSource, databaseConfig = config)
            databases[dbName] = database
            if (isDefault) {
                defaultDatabase = database
                TransactionManager.defaultDatabase = defaultDatabase
            }
            return database
        } catch (e: Throwable) {
            throw InternalServerException(
                InfraResponseCode.DB_ERROR,
                "fail to create exposed database $dbName",
                e
            )
        }
    }

    fun getDatabase(name: String): Database =
        databases[name] ?: throw InternalServerException(InfraResponseCode.DB_ERROR, "exposed database $name is not exist")

    fun getDefaultDatabase(name: String): Database = defaultDatabase

    private fun buildExposedDatabaseConfig(externalExposedDatabaseConfig: ExposedDatabaseConfig) =
        org.jetbrains.exposed.sql.DatabaseConfig {
            defaultMaxAttempts = externalExposedDatabaseConfig.defaultMaxAttempts
            useNestedTransactions = false
        }
}