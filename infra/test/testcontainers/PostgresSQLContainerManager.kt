/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package testcontainers

import fanpoll.infra.database.hikari.HikariConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.ConcurrentHashMap

/**
references
1. https://www.testcontainers.org/features/configuration/
2. https://rieckpil.de/reuse-containers-with-testcontainers-for-fast-integration-tests/
3. https://github.com/testcontainers/testcontainers-java/issues/3780
※ set checks.disable=true and testcontainers.reuse.enable=true in $HOME/.testcontainers.properties
 */
object PostgresSQLContainerManager {

    private val containers = ConcurrentHashMap<String, PostgreSQLContainer<*>>()

    fun create(projectId: String = "infra", containerName: String = "default", hikariConfig: HikariConfig): PostgreSQLContainer<*> {
        check(!containers.contains(containerName)) { "$containerName exists" }

        val postgresImageName = System.getProperty("testcontainers.image.postgres", "postgres")
        val container = PostgreSQLContainer(DockerImageName.parse(postgresImageName))
            .withReuse(true)
            .withLabels(mapOf("project" to projectId, "name" to containerName))
            .withUsername(hikariConfig.username)
            .withPassword(hikariConfig.password)
            .withDatabaseName(parseDatabaseNameFromJdbcUrl(hikariConfig.jdbcUrl))
            .apply { start() }
        containers[containerName] = container
        return container
    }

    private fun parseDatabaseNameFromJdbcUrl(jdbcUrl: String): String =
        jdbcUrl.substringBefore('?').substringAfterLast('/')

    fun getContainer(containerName: String): GenericContainer<*> = containers[containerName] ?: error("$containerName not exists")
}