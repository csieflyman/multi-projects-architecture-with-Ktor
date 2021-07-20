/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.database

import com.zaxxer.hikari.HikariDataSource
import fanpoll.MyApplicationConfig
import fanpoll.infra.base.async.AsyncExecutorConfig
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.koin.KoinApplicationShutdownManager
import fanpoll.infra.base.response.ResponseCode
import fanpoll.infra.database.util.DBAsyncTaskCoroutineActor
import fanpoll.infra.logging.writers.LogWriter
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.koin
import org.jetbrains.exposed.sql.Database as ExposedDatabase

/**
 *  Support Flyway (database migration tool), Hikari (connection pool), Exposed (orm)
 */
class DatabaseFeature(configuration: Configuration) {

    class Configuration {

        internal val hikariConfig: com.zaxxer.hikari.HikariConfig = com.zaxxer.hikari.HikariConfig().apply {
            isAutoCommit = false
        }

        internal val flywayConfig: FluentConfiguration = FluentConfiguration().apply {
        }

        internal var asyncExecutorConfig: AsyncExecutorConfig? = null

        fun hikari(configure: com.zaxxer.hikari.HikariConfig.() -> Unit) {
            hikariConfig.apply(configure)
        }

        fun flyway(configure: FluentConfiguration.() -> Unit) {
            flywayConfig.apply(configure)
        }

        fun asyncExecutor(configure: AsyncExecutorConfig.Builder.() -> Unit) {
            asyncExecutorConfig = AsyncExecutorConfig.Builder().apply(configure).build()
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, DatabaseFeature> {

        override val key = AttributeKey<DatabaseFeature>("Database")

        private val logger = KotlinLogging.logger {}

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): DatabaseFeature {
            val configuration = Configuration().apply(configure)
            val feature = DatabaseFeature(configuration)

            val appConfig = pipeline.get<MyApplicationConfig>()
            if (appConfig.infra.database != null) {
                configure(configuration, appConfig.infra.database) // override config
            }

            connect(configuration.hikariConfig)
            migrate(configuration.flywayConfig)

            val asyncExecutorConfig = appConfig.infra.database?.asyncExecutor ?: configuration.asyncExecutorConfig
            if (asyncExecutorConfig != null) {
                initAsyncExecutor(pipeline, asyncExecutorConfig)
            }

            KoinApplicationShutdownManager.register { shutdown() }

            return feature
        }

        private var asyncExecutor: DBAsyncTaskCoroutineActor? = null

        private fun initAsyncExecutor(pipeline: Application, config: AsyncExecutorConfig) {
            val logWriter = pipeline.get<LogWriter>()
            asyncExecutor = DBAsyncTaskCoroutineActor(config.coroutineActor, logWriter)
            pipeline.koin {
                modules(
                    module(createdAtStart = true) {
                        single { asyncExecutor }
                    }
                )
            }
        }

        private fun configure(configuration: Configuration, databaseConfig: DatabaseConfig) {
            val externalHikariConfig = databaseConfig.hikari
            with(configuration.hikariConfig) {
                driverClassName = externalHikariConfig.driverClassName
                jdbcUrl = externalHikariConfig.jdbcUrl
                username = externalHikariConfig.username
                password = externalHikariConfig.password
                minimumIdle = externalHikariConfig.minimumIdle
                maximumPoolSize = externalHikariConfig.maximumPoolSize
                idleTimeout = externalHikariConfig.idleTimeout
                connectionTimeout = externalHikariConfig.connectionTimeout
            }

            val externalFlywayConfig = databaseConfig.flyway
            with(configuration.flywayConfig) {
                baselineOnMigrate(externalFlywayConfig.baselineOnMigrate)
                validateOnMigrate(externalFlywayConfig.validateOnMigrate)
                externalFlywayConfig.table?.let { table(it) }
            }
        }

        private lateinit var dataSource: HikariDataSource
        private lateinit var flyway: Flyway
        private lateinit var defaultDatabase: org.jetbrains.exposed.sql.Database

        private fun connect(config: com.zaxxer.hikari.HikariConfig) {
            try {
                logger.info("===== connect database ${config.jdbcUrl}... =====")
                dataSource = HikariDataSource(config)
                defaultDatabase = ExposedDatabase.connect(dataSource)
                logger.info("===== database connected =====")

                /**
                 * see https://github.com/JetBrains/Exposed/wiki/Transactions
                 * After that any exception that happens within transaction block will not rollback the whole transaction
                 * but only the code inside current transaction.
                 * Exposed uses SQL SAVEPOINT functionality to mark current transaction at the beginning of transaction block and release it on exit from it.
                 */
                defaultDatabase.useNestedTransactions = false
                defaultDatabase.transactionManager.defaultRepetitionAttempts = 0
            } catch (e: Throwable) {
                throw InternalServerException(
                    ResponseCode.DB_ERROR,
                    "fail to connect database connection pool! => ${config.jdbcUrl}", e
                )
            }
        }

        private fun migrate(config: FluentConfiguration) {
            try {
                logger.info("===== Flyway migrate... =====")
                flyway = config.dataSource(dataSource).load()
                flyway.migrate()
                logger.info("===== Flyway migrate finished =====")
            } catch (e: Throwable) {
                throw InternalServerException(ResponseCode.DB_ERROR, "fail to migrate database", e)
            }
        }

        private fun shutdown() {
            asyncExecutor?.shutdown()
            closeConnection()
        }

        private fun closeConnection() {
            try {
                if (dataSource.isRunning) {
                    logger.info("close database connection pool...")
                    dataSource.close()
                    logger.info("database connection pool closed")
                } else {
                    logger.warn("database connection pool had been closed")
                }
            } catch (e: Throwable) {
                throw InternalServerException(ResponseCode.DB_ERROR, "could not close database connection pool", e)
            }
        }
    }
}

data class DatabaseConfig(
    val hikari: HikariConfig,
    val flyway: FlywayConfig,
    val asyncExecutor: AsyncExecutorConfig? = null
)

data class HikariConfig(
    val driverClassName: String,
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val minimumIdle: Int,
    val maximumPoolSize: Int,
    val idleTimeout: Long,
    val connectionTimeout: Long
)

data class FlywayConfig(
    val baselineOnMigrate: Boolean = true,
    val validateOnMigrate: Boolean = true,
    val table: String? = null
)