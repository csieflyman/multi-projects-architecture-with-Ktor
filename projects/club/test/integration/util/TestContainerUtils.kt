/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.util

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

fun createPostgresContainer(): PostgreSQLContainer<*> {

    val postgresImageName = System.getProperty("testcontainers.image.postgres", "postgres")
// https://kotlinlang.org/docs/whatsnew1530.html#improvements-to-type-inference-for-recursive-generic-types
// COMPATIBILITY => update intellij kotlin plugin to early access preview 1.6.x
    return PostgreSQLContainer(DockerImageName.parse(postgresImageName))
        .withDatabaseName("test-db")
        .withUsername("tester")
        .withPassword("test")
}

fun createRedisContainer(): GenericContainer<*> {
    val redisImageName = System.getProperty("testcontainers.image.redis", "redis")
    return GenericContainer(DockerImageName.parse(redisImageName)).withExposedPorts(6379)
}



