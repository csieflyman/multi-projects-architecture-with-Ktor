/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.infra.database.exposed

data class ExposedDatabaseConfig(
    val defaultMaxAttempts: Int = 1
)