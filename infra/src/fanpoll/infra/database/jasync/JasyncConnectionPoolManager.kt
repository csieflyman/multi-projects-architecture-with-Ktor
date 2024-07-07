/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.jasync

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.ConnectionPoolConfigurationBuilder
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.jasync.sql.db.postgresql.util.URLParser
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

object JasyncConnectionPoolManager {

    private val logger = KotlinLogging.logger {}

    private val connections = ConcurrentHashMap<String, Connection>()
    private var closed: Boolean = false

    @Synchronized
    fun createConnectionPool(dbName: String, externalConfig: JasyncConfig): Connection {
        if (connections.containsKey(dbName))
            throw InternalServerException(
                InfraResponseCode.SERVER_CONFIG_ERROR,
                "duplicated jasync connection pool: $dbName"
            )

        val config = buildJasyncConfig(externalConfig)
        try {
            logger.info { "===== connect database with jasync ... =====" }
            val connection = ConnectionPool(PostgreSQLConnectionFactory(config.connectionConfiguration), config)
            connections[dbName] = connection
            logger.info { "===== jasync database connected =====" }
            return connection
        } catch (e: Throwable) {
            throw InternalServerException(InfraResponseCode.DB_ERROR, "fail to connect database With jasync", e)
        }
    }

    fun getConnection(name: String): Connection =
        connections[name] ?: throw InternalServerException(InfraResponseCode.DB_ERROR, "jasync connection $name is not exist")

    @Synchronized
    fun closeAllConnectionPools() {
        if (closed)
            return
        closed = true
        connections.values.forEach { connection ->
            try {
                logger.info { "close jasync database connection pool..." }
                connection.disconnect().get()
                logger.info { "jasync database connection pool closed" }
            } catch (e: Throwable) {
                logger.error(e) { "fail to close jasync database connection pool" }
                //throw InternalServerException(InfraResponseCode.DB_ERROR, "fail to close jasync database connection pool", e)
            }
        }
    }

    private fun buildJasyncConfig(externalJasyncConfig: JasyncConfig) = ConnectionPoolConfigurationBuilder().apply {
        val connectionConfig = URLParser.parse(externalJasyncConfig.jdbcUrl)
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
    }.build()
}