/*
 * Copyright (c) 2024. fanpoll All rights reserved.
 */

package fanpoll.ops.database

import fanpoll.infra.database.loadDatabaseModule
import fanpoll.ops.OpsKoinContext
import fanpoll.ops.database.exposed.OpsDatabase
import io.ktor.server.application.Application

fun Application.loadOpsDatabaseModule(databasesConfig: OpsDatabasesConfig) {
    databasesConfig.ops.name = OpsDatabase.Ops.name

    OpsKoinContext.koin.loadModules(
        listOf(loadDatabaseModule(listOf(databasesConfig.ops)))
    )
}