/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.hikari

import com.zaxxer.hikari.HikariDataSource
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

object HikariDataSourceManager {

    private val logger = KotlinLogging.logger {}

    private val dataSources = ConcurrentHashMap<String, HikariDataSource>()
    private val configs = ConcurrentHashMap<String, com.zaxxer.hikari.HikariConfig>()
    private var closed: Boolean = false

    @Synchronized
    fun createDataSource(dbName: String, externalConfig: HikariConfig): HikariDataSource {
        if (dataSources.containsKey(dbName))
            throw InternalServerException(
                InfraResponseCode.SERVER_CONFIG_ERROR,
                "duplicated datasource: ${dbName}"
            )
        val config = buildHikariConfig(externalConfig)
        configs[dbName] = config
        try {
            logger.info { "===== connect database ${config.jdbcUrl}... =====" }
            val dataSource = HikariDataSource(config)
            dataSources[dbName] = dataSource
            logger.info { "===== database connected =====" }
            return dataSource
        } catch (e: Throwable) {
            throw InternalServerException(InfraResponseCode.DB_ERROR, "fail to connect database: ${config.jdbcUrl}", e)
        }
    }

    fun getDatabaseSource(name: String): HikariDataSource =
        dataSources[name] ?: throw InternalServerException(InfraResponseCode.DB_ERROR, "datasource name $name is not exist")

    private fun buildHikariConfig(externalHikariConfig: HikariConfig) = com.zaxxer.hikari.HikariConfig().apply {
        isAutoCommit = false
        driverClassName = externalHikariConfig.driverClassName
        jdbcUrl = externalHikariConfig.jdbcUrl
        username = externalHikariConfig.username
        password = externalHikariConfig.password
        minimumIdle = externalHikariConfig.minimumIdle
        maximumPoolSize = externalHikariConfig.maximumPoolSize
        idleTimeout = externalHikariConfig.idleTimeout
        connectionTimeout = externalHikariConfig.connectionTimeout
    }

    @Synchronized
    fun closeAllDataSources() {
        if (closed)
            return
        closed = true
        dataSources.values.forEach { dataSource ->
            try {
                if (dataSource.isRunning) {
                    logger.info { "close database connection pool..." }
                    dataSource.close()
                    logger.info { "database connection pool closed" }
                } else {
                    logger.warn { "database connection pool had been closed" }
                }
            } catch (e: Throwable) {
                logger.error(e) { "fail to close database connection pool" }
                //throw InternalServerException(InfraResponseCode.DB_ERROR, "fail to close database connection pool", e)
            }
        }
    }

    fun getStatus(dataSourceName: String, logging: Boolean = true): String {
        val dataSource = dataSources[dataSourceName]!!
        val config = configs[dataSourceName]!!
        val output = """
        ===== DataSource ${dataSource.poolName} =====
        jdbcUrl: ${config.jdbcUrl}
        driverClassName: $${config.driverClassName}
        username: ${config.username}
        maximumPoolSize: ${dataSource.maximumPoolSize}
        minimumIdle: ${dataSource.minimumIdle}
        idleTimeout: ${dataSource.idleTimeout}
        maxLifetime: ${dataSource.maxLifetime}
        validationTimeout: ${dataSource.validationTimeout}
        loginTimeout: ${dataSource.loginTimeout}
        connectionTimeout: ${dataSource.connectionTimeout}
        connectionTestQuery: ${dataSource.connectionTestQuery}
        leakDetectionThreshold: ${dataSource.leakDetectionThreshold}
        initializationFailTimeout: ${dataSource.initializationFailTimeout}
        """.trimIndent()
        if (logging)
            logger.info { output }
        return output
    }
}