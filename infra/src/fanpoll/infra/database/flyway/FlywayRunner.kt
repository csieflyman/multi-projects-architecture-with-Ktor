/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.flyway

import com.zaxxer.hikari.HikariDataSource
import fanpoll.infra.base.exception.InternalServerException
import fanpoll.infra.base.response.InfraResponseCode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway

class FlywayRunner(private val flyway: Flyway) {

    private val logger = KotlinLogging.logger {}
    fun migrate() {
        try {
            logger.info { "===== Flyway migrate... ${(flyway.configuration.dataSource as HikariDataSource).poolName} =====" }
            flyway.migrate()
            logger.info { "===== Flyway migrate finished =====" }
        } catch (e: Throwable) {
            throw InternalServerException(InfraResponseCode.DB_ERROR, "fail to migrate database", e)
        }
    }
}