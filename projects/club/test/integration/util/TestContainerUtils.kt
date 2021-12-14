/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.util

import fanpoll.infra.database.HikariConfig
import fanpoll.infra.redis.RedisConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

object TestContainerUtils {

    fun createPostgresContainer(): PostgreSQLContainer<*> {
        val postgresImageName = System.getProperty("testcontainers.image.postgres", "postgres")
        // https://kotlinlang.org/docs/whatsnew1530.html#improvements-to-type-inference-for-recursive-generic-types
        // COMPATIBILITY => update intellij kotlin plugin to early access preview 1.6.x
        return PostgreSQLContainer(DockerImageName.parse(postgresImageName))
            .withDatabaseName("test-db")
            .withUsername("tester")
            .withPassword("test")
    }

    fun replacePostgresConfig(container: PostgreSQLContainer<*>, config: HikariConfig) {
        with(config) {
            jdbcUrl = container.jdbcUrl
            username = container.username
            password = container.password
        }
    }

    fun createRedisContainer(): GenericContainer<*> {
        val redisImageName = System.getProperty("testcontainers.image.redis", "redis")
        return GenericContainer(DockerImageName.parse(redisImageName)).withExposedPorts(6379)
    }

    fun replaceRedisConfig(container: GenericContainer<*>, config: RedisConfig) {
        with(config) {
            host = container.host
            port = container.firstMappedPort
        }
    }
}



