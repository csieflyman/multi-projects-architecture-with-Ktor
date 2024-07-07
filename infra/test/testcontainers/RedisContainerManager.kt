/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package testcontainers

import fanpoll.infra.redis.RedisConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.ConcurrentHashMap

/**
references
1. https://www.testcontainers.org/features/configuration/
2. https://rieckpil.de/reuse-containers-with-testcontainers-for-fast-integration-tests/
3. https://github.com/testcontainers/testcontainers-java/issues/3780
※ set checks.disable=true and testcontainers.reuse.enable=true in $HOME/.testcontainers.properties
 */

object RedisContainerManager {

    private val containers = ConcurrentHashMap<String, GenericContainer<*>>()

    fun create(projectId: String, containerName: String = "default", redisConfig: RedisConfig): GenericContainer<*> {
        check(!containers.contains(containerName)) { "$containerName exists" }

        val redisImageName = System.getProperty("testcontainers.image.redis", "redis")
        val container = GenericContainer(DockerImageName.parse(redisImageName))
            .withReuse(true)
            .withLabels(mapOf("project" to projectId, "name" to containerName))
            .withExposedPorts(redisConfig.port)
            .apply { start() }
        containers[containerName] = container
        return container
    }

    fun getContainer(containerName: String): GenericContainer<*> = containers[containerName] ?: error("$containerName not exists")
}