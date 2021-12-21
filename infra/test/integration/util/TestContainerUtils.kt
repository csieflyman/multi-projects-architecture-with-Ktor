/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

package integration.util

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

object TestContainerUtils {

    fun createPostgresContainer(databaseName: String): PostgreSQLContainer<*> {
        val postgresImageName = System.getProperty("testcontainers.image.postgres", "postgres")
        // https://kotlinlang.org/docs/whatsnew1530.html#improvements-to-type-inference-for-recursive-generic-types
        // COMPATIBILITY => update intellij kotlin plugin to early access preview 1.6.x
        return PostgreSQLContainer(DockerImageName.parse(postgresImageName))
            .withDatabaseName(databaseName)
            .withUsername("tester")
            .withPassword("test")
    }
}



