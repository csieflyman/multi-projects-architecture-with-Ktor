/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.util

import fanpoll.infra.database.HikariConfig
import fanpoll.infra.redis.RedisConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

object SinglePostgreSQLContainer {

    val instance: PostgreSQLContainer<*> by lazy {
        val postgresImageName = System.getProperty("testcontainers.image.postgres", "postgres")
        // https://kotlinlang.org/docs/whatsnew1530.html#improvements-to-type-inference-for-recursive-generic-types
        // COMPATIBILITY => update intellij kotlin plugin to early access preview 1.6.x
        PostgreSQLContainer(DockerImageName.parse(postgresImageName))
            .withReuse(true)
            .withUsername("tester")
            .withPassword("test")
            .withDatabaseName("test-default") // only one database now
        //.withInitScript("init_test_db_container.sql") // script for creating multiple databases in a container
    }

    fun configureConnectionProperties(config: HikariConfig) {
        with(config) {
            jdbcUrl = instance.jdbcUrl
            username = instance.username
            password = instance.password
        }
    }
}

object SingleRedisContainer {

    val instance: GenericContainer<*> by lazy {
        val redisImageName = System.getProperty("testcontainers.image.redis", "redis")
        GenericContainer(DockerImageName.parse(redisImageName))
            .withReuse(true)
            .withExposedPorts(6379)
    }

    fun configureConnectionProperties(config: RedisConfig) {
        with(config) {
            host = instance.host
            port = instance.firstMappedPort
        }
    }
}



