/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fanpoll.infra.InternalServerErrorException
import fanpoll.infra.ResponseCode
import fanpoll.infra.logging.ErrorLogDTO
import fanpoll.infra.logging.LogManager
import fanpoll.infra.logging.LogMessage
import fanpoll.infra.logging.LogType
import fanpoll.infra.utils.CoroutineConfig
import fanpoll.infra.utils.CoroutineUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.jetbrains.exposed.sql.Database as ExposedDatabase

/**
 *  use Flyway (database migration tool), Hikari (connection pool), Exposed (orm)
 */
object DatabaseManager {

    private val logger = KotlinLogging.logger {}

    private lateinit var dataSource: HikariDataSource
    private lateinit var flyway: Flyway
    private lateinit var default: org.jetbrains.exposed.sql.Database

    private const val dispatcherName = "DB"
    private lateinit var dispatcher: CoroutineDispatcher
    private lateinit var channel: SendChannel<DBAsyncTask>
    private lateinit var coroutineScope: CoroutineScope
    private var coroutineEnabled: Boolean = false

    class Configuration(private val databaseConfig: DatabaseConfig) {

        val hikariConfig: HikariConfig = HikariConfig().apply {
            val myConfig = databaseConfig.hikari
            isAutoCommit = false
            driverClassName = myConfig.driverClassName
            jdbcUrl = myConfig.jdbcUrl
            username = myConfig.username
            password = myConfig.password
            minimumIdle = myConfig.minimumIdle
            maximumPoolSize = myConfig.maximumPoolSize
            idleTimeout = myConfig.idleTimeout
            connectionTimeout = myConfig.connectionTimeout
        }

        val flywayConfig: FluentConfiguration = FluentConfiguration().apply {
            val myConfig = databaseConfig.flyway
            myConfig.baselineOnMigrate?.let { this@apply.baselineOnMigrate(it) }
            myConfig.table?.let { this@apply.table(it) }
        }

        fun hikari(configure: HikariConfig.() -> Unit) {
            hikariConfig.apply(configure)
        }

        fun flyway(configure: FluentConfiguration.() -> Unit) {
            flywayConfig.apply(configure)
        }
    }

    fun init(databaseConfig: DatabaseConfig, configure: (Configuration.() -> Unit)? = null) {
        logger.info("========== init DatabaseManager... ==========")
        val config = Configuration(databaseConfig)
        configure?.let { config.apply(it) }

        connect(config)
        migrate()

        if (databaseConfig.coroutine != null) {
            coroutineEnabled = true
            dispatcher = if (databaseConfig.coroutine.dispatcher != null)
                CoroutineUtils.createDispatcher(dispatcherName, databaseConfig.coroutine.dispatcher)
            else Dispatchers.IO

            val context = dispatcher // TODO coroutine exception handling
            coroutineScope = CoroutineScope(context)
            channel = CoroutineUtils.createActor(
                dispatcherName, databaseConfig.coroutine.coroutines,
                coroutineScope, channelBlock
            )
        }
        logger.info("========== init DatabaseManager completed ==========")
    }

    private fun connect(config: Configuration) {
        try {
            dataSource = HikariDataSource(config.hikariConfig)
            flyway = config.flywayConfig.dataSource(dataSource).load()
            default = ExposedDatabase.connect(dataSource)
            default.transactionManager.defaultRepetitionAttempts = 0
            /**
             * see https://github.com/JetBrains/Exposed/wiki/Transactions
             * After that any exception that happens within transaction block will not rollback the whole transaction
             * but only the code inside current transaction.
             * Exposed uses SQL SAVEPOINT functionality to mark current transaction at the beginning of transaction block and release it on exit from it.
             */
            default.useNestedTransactions = false
        } catch (e: Throwable) {
            throw InternalServerErrorException(
                ResponseCode.DB_ERROR,
                "fail to connect database connection pool! => ${config.hikariConfig.jdbcUrl}", e
            )
        }
    }

    private fun migrate() {
        try {
            logger.info("===== Flyway migrate... =====")
            flyway.migrate()
            logger.info("===== Flyway migrate completed =====")
        } catch (e: Throwable) {
            throw InternalServerErrorException(ResponseCode.DB_ERROR, "fail to migrate database", e)
        }
    }

    fun shutdown() {
        logger.info("shutdown DatabaseManager...")
        closeCoroutine()
        closeConnection()
        logger.info("shutdown DatabaseManager completed")
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
            throw InternalServerErrorException(ResponseCode.DB_ERROR, "could not close database connection pool", e)
        }
    }

    private fun closeCoroutine() {
        CoroutineUtils.closeChannel(dispatcherName, channel)
        coroutineScope.cancel(dispatcherName)
        if (coroutineEnabled && dispatcher is ExecutorCoroutineDispatcher) {
            CoroutineUtils.closeDispatcher(dispatcherName, dispatcher as ExecutorCoroutineDispatcher)
        }
    }

    suspend fun execute(type: String, block: Transaction.() -> Any?) {
        require(coroutineEnabled)
        channel.send(DBAsyncTask(type, block))
    }

    fun executeAsync(type: String, block: Transaction.() -> Any?) {
        runBlocking {
            execute(type, block)
        }
    }

    private val channelBlock: suspend (DBAsyncTask) -> Unit = { task ->
        try {
            transaction {
                task.block(this)
            }
        } catch (e: Throwable) {
            LogManager.writeAsync(
                LogMessage(
                    LogType.SERVER_ERROR,
                    ErrorLogDTO.internal(
                        InternalServerErrorException(ResponseCode.DB_ASYNC_ERROR, null, e),
                        "DB_ASYNC", task.type
                    )
                )
            )
        }
    }
}

class DBAsyncTask(val type: String, val block: Transaction.() -> Any?)

data class DatabaseConfig(
    val hikari: MyHikariConfig,
    val flyway: MyFlywayConfig,
    val coroutine: CoroutineConfig?
)

data class MyHikariConfig(
    val driverClassName: String,
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val minimumIdle: Int,
    val maximumPoolSize: Int,
    val idleTimeout: Long,
    val connectionTimeout: Long
)

data class MyFlywayConfig(
    val baselineOnMigrate: Boolean?,
    val table: String?
)