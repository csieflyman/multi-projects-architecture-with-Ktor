/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.flyway

data class FlywayConfig(
    val baselineOnMigrate: Boolean = true,
    val validateOnMigrate: Boolean = true,
    val locations: String? = null,
    val table: String? = null
)