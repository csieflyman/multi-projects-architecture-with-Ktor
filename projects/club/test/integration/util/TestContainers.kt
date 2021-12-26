/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.util

import fanpoll.infra.MyApplicationConfig
import integration.util.SingleTestContainer.Companion.PROJECT_LABELS
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
references
1. https://www.testcontainers.org/features/configuration/
2. https://rieckpil.de/reuse-containers-with-testcontainers-for-fast-integration-tests/
3. https://github.com/testcontainers/testcontainers-java/issues/3780
※ set checks.disable=true and testcontainers.reuse.enable=true in $HOME/.testcontainers.properties
 */

interface SingleTestContainer {

    val instance: GenericContainer<*>

    fun configure(appConfig: MyApplicationConfig)

    companion object {
        val PROJECT_LABELS = mapOf("project" to "club")
    }
}

object SinglePostgreSQLContainer : SingleTestContainer {

    override val instance: PostgreSQLContainer<*> by lazy {
        val postgresImageName = System.getProperty("testcontainers.image.postgres", "postgres")
        PostgreSQLContainer(DockerImageName.parse(postgresImageName))
            .withReuse(true).withLabels(PROJECT_LABELS)
            .withUsername("tester")
            .withPassword("test")
            .withDatabaseName("test-default") // only one database now
            //.withInitScript("init_test_db_container.sql") // script for creating multiple databases in a container
            .apply { start() }
    }

    override fun configure(appConfig: MyApplicationConfig) {
        with(appConfig.infra.database!!.hikari) {
            jdbcUrl = instance.jdbcUrl
            username = instance.username
            password = instance.password
        }
    }
}

object SingleRedisContainer : SingleTestContainer {

    override val instance: GenericContainer<*> by lazy {
        val redisImageName = System.getProperty("testcontainers.image.redis", "redis")
        GenericContainer(DockerImageName.parse(redisImageName))
            .withReuse(true).withLabels(PROJECT_LABELS)
            .withExposedPorts(6379)
            .apply { start() }
    }

    override fun configure(appConfig: MyApplicationConfig) {
        with(appConfig.infra.redis!!) {
            host = instance.host
            port = instance.firstMappedPort
        }
    }
}



