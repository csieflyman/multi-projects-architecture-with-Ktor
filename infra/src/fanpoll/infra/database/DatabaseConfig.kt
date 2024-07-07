/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database

import fanpoll.infra.database.exposed.ExposedDatabaseConfig
import fanpoll.infra.database.flyway.FlywayConfig
import fanpoll.infra.database.hikari.HikariConfig
import fanpoll.infra.database.jasync.JasyncConfig

data class DatabaseConfig(
    var hikari: HikariConfig, // declare var for testcontainers replace it
    val exposed: ExposedDatabaseConfig,
    val flyway: FlywayConfig,
    val jasync: JasyncConfig? = null
) {
    lateinit var name: String
    var isDefault: Boolean = false
}