/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.flyway

import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import org.flywaydb.core.api.configuration.FluentConfiguration
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

object FlywayRunnerFactory {

    private val runners = ConcurrentHashMap<String, FlywayRunner>()
    fun create(dbName: String, dataSource: DataSource, externalFlywayConfig: FlywayConfig): FlywayRunner {
        if (runners.containsKey(dbName))
            throw InternalServerException(
                InfraResponseCode.SERVER_CONFIG_ERROR,
                "duplicated flyway runner: $dbName"
            )
        val flywayConfig = buildFlywayConfig(externalFlywayConfig)
        val flyway = flywayConfig.dataSource(dataSource).load()
        val runner = FlywayRunner(flyway)
        runners[dbName] = runner
        return runner
    }

    private fun buildFlywayConfig(externalFlywayConfig: FlywayConfig) = FluentConfiguration().apply {
        baselineOnMigrate(externalFlywayConfig.baselineOnMigrate)
        validateOnMigrate(externalFlywayConfig.validateOnMigrate)
        externalFlywayConfig.locations?.let { locations(*it.split(",").toTypedArray()) }
        externalFlywayConfig.table?.let { table(it) }
    }
}