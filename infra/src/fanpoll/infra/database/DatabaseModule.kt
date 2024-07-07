/*
 * Copyright (c) 2020. fanpoll All rights reserved.
 */

package fanpoll.infra.database

import com.github.jasync.sql.db.Connection
import fanpoll.infra.database.exposed.ExposedDatabaseManager
import fanpoll.infra.database.exposed.jasync.JasyncExposedAdapter
import fanpoll.infra.database.flyway.FlywayRunnerFactory
import fanpoll.infra.database.hikari.HikariDataSourceManager
import fanpoll.infra.database.jasync.JasyncConnectionPoolManager
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val logger = KotlinLogging.logger {}

fun Application.loadDatabaseModule(configs: List<DatabaseConfig>): Module =
    module(createdAtStart = true) {
        configs.forEach { config ->
            val dataSource = HikariDataSourceManager.createDataSource(config.name, config.hikari)

            val exposedDatabase = ExposedDatabaseManager.createDatabase(config.name, config.isDefault, dataSource, config.exposed)
            single<Database>(named(config.name)) { exposedDatabase }

            val flywayRunner = FlywayRunnerFactory.create(config.name, dataSource, config.flyway)
            flywayRunner.migrate()

            if (config.jasync != null) {
                val jasyncConnection = JasyncConnectionPoolManager.createConnectionPool(config.name, config.jasync)
                single<Connection>(named(config.name)) { jasyncConnection }
                JasyncExposedAdapter.bind(exposedDatabase, jasyncConnection)
            }
        }
    }