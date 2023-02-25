/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.database

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.ConnectionPoolConfiguration
import com.github.jasync.sql.db.ConnectionPoolConfigurationBuilder
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.jasync.sql.db.postgresql.util.URLParser
import com.zaxxer.hikari.HikariDataSource
import fanpoll.infra.MyApplicationConfig
import fanpoll.infra.base.async.AsyncExecutorConfig
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.koin.KoinApplicationShutdownManager
import fanpoll.infra.base.response.InfraResponseCode
import fanpoll.infra.database.jasync.JasyncExposedAdapter
import fanpoll.infra.database.util.DBAsyncTaskCoroutineActor
import fanpoll.infra.logging.writers.LogWriter
import io.ktor.server.application.Application
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.util.AttributeKey
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.koin
import org.jetbrains.exposed.sql.Database as ExposedDatabase

/**
 *  Support Flyway (database migration tool), Hikari (connection pool), Exposed (orm)
 */
class DatabasePlugin(configuration: Configuration) {

    class Configuration {

        internal val hikariConfig: com.zaxxer.hikari.HikariConfig = com.zaxxer.hikari.HikariConfig().apply {
            isAutoCommit = false
        }

        fun hikari(configure: com.zaxxer.hikari.HikariConfig.() -> Unit) {
            hikariConfig.apply(configure)
        }

        internal val flywayConfig: FluentConfiguration = FluentConfiguration().apply {}

        fun flyway(configure: FluentConfiguration.() -> Unit) {
            flywayConfig.apply(configure)
        }

        internal var asyncExecutorConfig: AsyncExecutorConfig? = null

        fun asyncExecutor(configure: AsyncExecutorConfig.Builder.() -> Unit) {
            asyncExecutorConfig = AsyncExecutorConfig.Builder().apply(configure).build()
        }

        var jasyncConfig: ConnectionPoolConfiguration? = null

        fun jasync(configure: ConnectionPoolConfigurationBuilder.() -> Unit) {
            jasyncConfig = ConnectionPoolConfigurationBuilder().apply(configure).build()
        }
    }

    companion object Plugin : BaseApplicationPlugin<Application, Configuration, DatabasePlugin> {

        override val key = AttributeKey<DatabasePlugin>("Database")

        private val logger = KotlinLogging.logger {}

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): DatabasePlugin {
            val configuration = Configuration().apply(configure)
            val plugin = DatabasePlugin(configuration)

            val appConfig = pipeline.get<MyApplicationConfig>()
            if (appConfig.infra.database != null) {
                configure(configuration, appConfig.infra.database) // override config
            }

            connectWithHikari(configuration.hikariConfig)

            initExposed()

            flywayMigrate(configuration.flywayConfig)

            jasyncEnabled = configuration.jasyncConfig != null
            if (jasyncEnabled) {
                initJasync(pipeline, configuration.jasyncConfig!!)
            }

            val asyncExecutorConfig = appConfig.infra.database?.asyncExecutor ?: configuration.asyncExecutorConfig
            if (asyncExecutorConfig != null) {
                initAsyncExecutor(pipeline, asyncExecutorConfig)
            }

            KoinApplicationShutdownManager.register { shutdown() }

            return plugin
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
            val externalJasyncConfig = databaseConfig.jasync
            if (externalJasyncConfig != null) {
                val builder = ConnectionPoolConfigurationBuilder()
                val connectionConfig = URLParser.parse(externalJasyncConfig.jdbcUrl)
                with(builder) {
                    host = connectionConfig.host
                    port = connectionConfig.port
                    database = connectionConfig.database

                    username = externalJasyncConfig.username
                    password = externalJasyncConfig.password
                    maxActiveConnections = externalJasyncConfig.maxActiveConnections
                    maxIdleTime = externalJasyncConfig.maxIdleTime
                    connectionCreateTimeout = externalJasyncConfig.connectionCreateTimeout
                    connectionTestTimeout = externalJasyncConfig.connectionTestTimeout
                    queryTimeout = externalJasyncConfig.queryTimeout
                }
                configuration.jasyncConfig = builder.build()
            }

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

        private fun connectWithHikari(config: com.zaxxer.hikari.HikariConfig) {
            try {
                logger.info("===== connect database ${config.jdbcUrl}... =====")
                dataSource = HikariDataSource(config)
                logger.info("===== database connected =====")
            } catch (e: Throwable) {
                throw InternalServerException(InfraResponseCode.DB_ERROR, "fail to connect database: ${config.jdbcUrl}", e)
            }
        }

        private fun initExposed() {
            try {
                defaultDatabase = ExposedDatabase.connect(dataSource)
                /**
                 * see https://github.com/JetBrains/Exposed/wiki/Transactions
                 * After that any exception that happens within transaction block will not rollback the whole transaction
                 * but only the code inside current transaction.
                 * Exposed uses SQL SAVEPOINT functionality to mark current transaction at the beginning of transaction block and release it on exit from it.
                 */
                defaultDatabase.useNestedTransactions = false
                defaultDatabase.transactionManager.defaultRepetitionAttempts = 0
            } catch (e: Throwable) {
                throw InternalServerException(InfraResponseCode.DB_ERROR, "fail to init Exposed", e)
            }
        }

        private fun flywayMigrate(config: FluentConfiguration) {
            try {
                logger.info("===== Flyway migrate... =====")
                flyway = config.dataSource(dataSource).load()
                flyway.migrate()
                logger.info("===== Flyway migrate finished =====")
            } catch (e: Throwable) {
                throw InternalServerException(InfraResponseCode.DB_ERROR, "fail to migrate database", e)
            }
        }

        private fun shutdown() {
            asyncExecutor?.shutdown()
            if (jasyncEnabled) {
                closeJasyncConnectionPool()
            }
            closeHikariConnectionPool()
        }

        private fun closeHikariConnectionPool() {
            try {
                if (dataSource.isRunning) {
                    logger.info("close database connection pool...")
                    dataSource.close()
                    logger.info("database connection pool closed")
                } else {
                    logger.warn("database connection pool had been closed")
                }
            } catch (e: Throwable) {
                logger.error("fail to close database connection pool", e)
                //throw InternalServerException(InfraResponseCode.DB_ERROR, "fail to close database connection pool", e)
            }
        }

        private var jasyncEnabled = false
        private lateinit var jasyncConnection: Connection

        private fun initJasync(pipeline: Application, config: ConnectionPoolConfiguration) {
            connectWithJasync(config)

            pipeline.koin {
                modules(
                    module(createdAtStart = true) {
                        single { jasyncConnection }
                    }
                )
            }

            JasyncExposedAdapter.bind(defaultDatabase, jasyncConnection)
        }

        private fun connectWithJasync(config: ConnectionPoolConfiguration) {
            try {
                logger.info("===== connect database with jasync ... =====")
                jasyncConnection = ConnectionPool(
                    PostgreSQLConnectionFactory(config.connectionConfiguration), config
                )
                jasyncConnection.connect().get()
                logger.info("===== jasync database connected =====")
            } catch (e: Throwable) {
                throw InternalServerException(InfraResponseCode.DB_ERROR, "fail to connect database With jasync", e)
            }
        }

        private fun closeJasyncConnectionPool() {
            try {
                logger.info("close jasync database connection pool...")
                jasyncConnection.disconnect().get()
                logger.info("jasync database connection pool closed")
            } catch (e: Throwable) {
                logger.error("fail to close jasync database connection pool", e)
                //throw InternalServerException(InfraResponseCode.DB_ERROR, "fail to close jasync database connection pool", e)
            }
        }
    }
}

data class DatabaseConfig(
    val hikari: HikariConfig,
    val flyway: FlywayConfig,
    val asyncExecutor: AsyncExecutorConfig? = null,
    val jasync: JasyncConfig? = null
)

// jdbcUrl, username, password are mutable for integration test by TestContainers
data class HikariConfig(
    val driverClassName: String,
    var jdbcUrl: String,
    var username: String,
    var password: String,
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

data class JasyncConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maxActiveConnections: Int,
    val maxIdleTime: Long,
    val connectionCreateTimeout: Long,
    val connectionTestTimeout: Long,
    val queryTimeout: Long? = null
)